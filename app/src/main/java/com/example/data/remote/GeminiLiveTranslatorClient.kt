package com.example.data.remote

import android.util.Base64
import com.example.audio.WavEncoder
import com.example.domain.model.AppSettings
import com.example.domain.model.SubtitleStyleMode
import com.example.domain.model.TranslationEngineMode
import com.example.domain.model.TranslatorStatus
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject

class GeminiLiveTranslatorClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Required for long-lived WebSockets
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
) {
    interface Listener {
        fun onStatus(status: TranslatorStatus, message: String)
        fun onPartialSubtitle(burmeseText: String)
        fun onCompletedSubtitle(burmeseText: String)
        fun onError(errorMessage: String)
    }

    private var webSocket: WebSocket? = null
    private var restWorkerJob: Job? = null
    private var restChannel: Channel<ByteArray>? = null
    private val isSetupComplete = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private var currentEngine: TranslationEngineMode = TranslationEngineMode.LIVE_WEBSOCKET
    private val currentTurnBuffer = StringBuilder()
    private val restBuffer = ByteArrayOutputStream()
    private var restActiveSpeechFrames = 0
    private var restSilenceFrames = 0
    private var lastContextLines = ArrayDeque<String>()

    fun start(
        scope: CoroutineScope,
        apiKey: String,
        settings: AppSettings,
        listener: Listener
    ) {
        stop()
        if (apiKey.isBlank()) {
            listener.onError("Missing Gemini API key. Please save your key in Settings.")
            return
        }

        isRunning.set(true)
        currentEngine = settings.engineMode
        currentTurnBuffer.clear()
        lastContextLines.clear()

        when (settings.engineMode) {
            TranslationEngineMode.LIVE_WEBSOCKET -> startLiveWebSocket(apiKey, settings, listener)
            TranslationEngineMode.CHUNKED_REST -> startChunkedRest(scope, apiKey, settings, listener)
        }
    }

    fun sendPcmAudio(pcmBytes: ByteArray, rmsNormalized: Float) {
        if (!isRunning.get()) return
        when (currentEngine) {
            TranslationEngineMode.LIVE_WEBSOCKET -> {
                val ws = webSocket ?: return
                if (!isSetupComplete.get()) return
                val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
                val payload = JSONObject().apply {
                    put("realtimeInput", JSONObject().apply {
                        put("mediaChunks", JSONArray().apply {
                            put(JSONObject().apply {
                                put("mimeType", "audio/pcm;rate=16000")
                                put("data", b64)
                            })
                        })
                    })
                }
                ws.send(payload.toString())
            }

            TranslationEngineMode.CHUNKED_REST -> {
                synchronized(restBuffer) {
                    val isSpeech = rmsNormalized > 0.035f
                    if (isSpeech) {
                        restActiveSpeechFrames++
                        restSilenceFrames = 0
                    } else if (restActiveSpeechFrames > 0) {
                        restSilenceFrames++
                    }

                    if (restActiveSpeechFrames > 0) {
                        restBuffer.write(pcmBytes)
                    }

                    // Flush segment after ~2.4s (76KB) or ~600ms of silence following speech (>0.6s audio)
                    val byteCount = restBuffer.size()
                    val minBytesForSilenceFlush = 16000 * 2 * 6 / 10 // ~0.6s
                    val maxSegmentBytes = 16000 * 2 * 25 / 10 // ~2.5s
                    val shouldFlush = (byteCount >= maxSegmentBytes) ||
                        (restSilenceFrames >= 6 && byteCount >= minBytesForSilenceFlush)

                    if (shouldFlush) {
                        val segment = restBuffer.toByteArray()
                        restBuffer.reset()
                        restActiveSpeechFrames = 0
                        restSilenceFrames = 0
                        restChannel?.trySend(segment)
                    }
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        isSetupComplete.set(false)
        currentTurnBuffer.clear()
        synchronized(restBuffer) {
            restBuffer.reset()
            restActiveSpeechFrames = 0
            restSilenceFrames = 0
        }
        restChannel?.close()
        restChannel = null
        restWorkerJob?.cancel()
        restWorkerJob = null
        webSocket?.close(1000, "Stopped by user")
        webSocket = null
    }

    private fun buildSystemInstruction(style: SubtitleStyleMode): String {
        val styleGuidance = when (style) {
            SubtitleStyleMode.CONCISE_SUBTITLES ->
                "Keep subtitles concise, punchy, and easy to read quickly while watching video (1-2 short lines)."
            SubtitleStyleMode.FULL_SENTENCES ->
                "Translate into complete, smooth, conversational spoken Burmese sentences."
        }
        return """
            You are a real-time live audio subtitle translator.
            Listen to the incoming audio stream in any spoken language (English, Japanese, Korean, Chinese, Thai, etc.) and translate it immediately into natural, idiomatic Burmese (Myanmar Unicode).
            Rules:
            1. Output ONLY the Burmese translation text.
            2. Never reply to the speaker, never explain, never add quotation marks, timestamps, Romanized text, or source-language words unless proper nouns.
            3. $styleGuidance
            4. If there is only silence, music without lyrics, or unintelligible noise, output nothing.
        """.trimIndent()
    }

    private fun startLiveWebSocket(
        apiKey: String,
        settings: AppSettings,
        listener: Listener
    ) {
        listener.onStatus(TranslatorStatus.CONNECTING, "Connecting to Gemini Live…")
        val url = "wss://generativelanguage.googleapis.com/ws/" +
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()
        isSetupComplete.set(false)

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isRunning.get()) {
                    webSocket.close(1000, "Cancelled")
                    return
                }
                val modelName = settings.liveModelId.removePrefix("models/")
                val isNativeAudio = modelName.contains("native-audio", ignoreCase = true)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    if (isNativeAudio) {
                        // Native audio models emit text via outputAudioTranscription
                        put("responseModalities", JSONArray().put("AUDIO"))
                    } else {
                        put("responseModalities", JSONArray().put("TEXT"))
                    }
                }

                val setupObj = JSONObject().apply {
                    put("model", "models/$modelName")
                    put("generationConfig", generationConfig)
                    if (isNativeAudio) {
                        put("outputAudioTranscription", JSONObject())
                    }
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", buildSystemInstruction(settings.subtitleStyle))
                            })
                        })
                    })
                }

                val envelope = JSONObject().put("setup", setupObj)
                webSocket.send(envelope.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketJson(text, listener)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleWebSocketJson(bytes.utf8(), listener)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (isRunning.get() && code != 1000) {
                    val hint = if (reason.isBlank()) "Connection closed ($code)" else "Closed ($code): $reason"
                    listener.onError(hint)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isRunning.get()) return
                val codeInfo = response?.code?.let { " (HTTP $it)" }.orEmpty()
                listener.onError(
                    "Live WebSocket error$codeInfo: ${t.localizedMessage ?: "Connection failed"}. " +
                        "Tip: Try 'Gemini Flash Audio Stream' in Settings if your model doesn't support Live WebSocket."
                )
            }
        })
    }

    private fun handleWebSocketJson(rawJson: String, listener: Listener) {
        if (!isRunning.get()) return
        try {
            val json = JSONObject(rawJson)

            val errorObj = json.optJSONObject("error")
            if (errorObj != null) {
                val msg = errorObj.optString("message", "Gemini Live API returned an error.")
                listener.onError(msg)
                return
            }

            if (json.has("setupComplete")) {
                isSetupComplete.set(true)
                listener.onStatus(TranslatorStatus.LISTENING, "Live listening • Burmese subtitles active")
                return
            }

            val serverContent = json.optJSONObject("serverContent") ?: return

            if (serverContent.optBoolean("interrupted", false)) {
                flushTurnBuffer(listener)
            }

            // 1) Parse text from modelTurn.parts
            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val textChunk = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (textChunk.isNotEmpty()) {
                        appendDelta(textChunk, listener)
                    }
                }
            }

            // 2) Parse outputAudioTranscription (for native-audio models)
            val outputTranscription = serverContent.optJSONObject("outputTranscription")
            val transcriptText = outputTranscription?.optString("text").orEmpty()
            if (transcriptText.isNotEmpty()) {
                appendDelta(transcriptText, listener)
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                flushTurnBuffer(listener)
                listener.onStatus(TranslatorStatus.LISTENING, "Live listening • Burmese subtitles active")
            }
        } catch (_: Exception) {
            // Ignore malformed incremental frame
        }
    }

    private fun appendDelta(delta: String, listener: Listener) {
        currentTurnBuffer.append(delta)
        val preview = currentTurnBuffer.toString().trim()
        if (preview.isNotEmpty()) {
            listener.onStatus(TranslatorStatus.TRANSLATING, "Translating to Burmese…")
            listener.onPartialSubtitle(preview)
        }
    }

    private fun flushTurnBuffer(listener: Listener) {
        val finalLine = currentTurnBuffer.toString().trim()
        currentTurnBuffer.clear()
        if (finalLine.isNotEmpty()) {
            listener.onCompletedSubtitle(finalLine)
        }
    }

    private fun startChunkedRest(
        scope: CoroutineScope,
        apiKey: String,
        settings: AppSettings,
        listener: Listener
    ) {
        val channel = Channel<ByteArray>(capacity = 4)
        restChannel = channel
        isSetupComplete.set(true)
        listener.onStatus(TranslatorStatus.LISTENING, "Audio stream active • Burmese subtitles ready")

        val restClient = client.newBuilder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        restWorkerJob = scope.launch(Dispatchers.IO) {
            for (pcmSegment in channel) {
                if (!isActive || !isRunning.get()) break
                listener.onStatus(TranslatorStatus.TRANSLATING, "Translating segment…")
                val wavBytes = WavEncoder.pcmToWav(pcmSegment)
                val b64Wav = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

                val contextNote = if (lastContextLines.isNotEmpty()) {
                    "Recent previous Burmese subtitles for continuity (do not repeat): ${lastContextLines.joinToString(" | ")}"
                } else {
                    ""
                }

                val modelId = settings.restModelId.removePrefix("models/")
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "$modelId:generateContent?key=$apiKey"

                val bodyJson = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(
                            JSONObject().put("text", buildSystemInstruction(settings.subtitleStyle))
                        ))
                    })
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/wav")
                                    put("data", b64Wav)
                                })
                            })
                            put(JSONObject().apply {
                                put(
                                    "text",
                                    "Translate spoken speech in this audio clip into natural Burmese subtitle text only. $contextNote".trim()
                                )
                            })
                        })
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                        put("maxOutputTokens", 180)
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                try {
                    restClient.newCall(request).execute().use { response ->
                        val respText = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            val errMsg = runCatching {
                                JSONObject(respText).optJSONObject("error")?.optString("message")
                            }.getOrNull() ?: "Gemini REST error (${response.code})"
                            listener.onError(errMsg)
                            return@use
                        }

                        val json = JSONObject(respText)
                        val candidates = json.optJSONArray("candidates")
                        val parts = candidates?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                        val translated = parts?.optJSONObject(0)?.optString("text")?.trim().orEmpty()

                        if (translated.isNotEmpty() &&
                            !translated.equals("silence", ignoreCase = true) &&
                            !translated.equals("none", ignoreCase = true)
                        ) {
                            lastContextLines.addLast(translated)
                            if (lastContextLines.size > 2) lastContextLines.removeFirst()
                            listener.onCompletedSubtitle(translated)
                        }
                        if (isRunning.get()) {
                            listener.onStatus(TranslatorStatus.LISTENING, "Listening • Burmese subtitles active")
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        listener.onError("Network error: ${e.localizedMessage ?: "Failed to translate segment"}")
                    }
                }
            }
        }
    }
}
