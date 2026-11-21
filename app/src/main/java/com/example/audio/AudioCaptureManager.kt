package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.domain.model.AudioSourceMode
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioCaptureManager {

    companion object {
        const val SAMPLE_RATE_HZ = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_DURATION_MS = 100 // 100ms = 3200 bytes at 16kHz 16-bit mono
        private const val CHUNK_BYTES = (SAMPLE_RATE_HZ * 2 * CHUNK_DURATION_MS) / 1000
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startCapture(
        scope: CoroutineScope,
        sourceMode: AudioSourceMode,
        mediaProjection: MediaProjection?,
        onAudioChunk: (pcmBytes: ByteArray, rmsNormalized: Float) -> Unit,
        onError: (String) -> Unit
    ) {
        stopCapture()

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            onError("Audio hardware does not support 16kHz PCM capture.")
            return
        }
        val bufferSize = maxOf(minBuffer * 2, CHUNK_BYTES * 4)

        val record = try {
            when (sourceMode) {
                AudioSourceMode.INTERNAL_MEDIA -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        onError("Internal media audio capture requires Android 10 (API 29) or higher.")
                        return
                    }
                    if (mediaProjection == null) {
                        onError("MediaProjection permission is required for internal media audio.")
                        return
                    }
                    buildPlaybackAudioRecord(mediaProjection, bufferSize)
                }

                AudioSourceMode.MICROPHONE -> {
                    AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AUDIO_FORMAT)
                                .setSampleRate(SAMPLE_RATE_HZ)
                                .setChannelMask(CHANNEL_CONFIG)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .build()
                }
            }
        } catch (se: SecurityException) {
            onError("Audio permission missing: ${se.localizedMessage ?: "RECORD_AUDIO denied"}")
            return
        } catch (e: Exception) {
            onError("Failed to initialize audio recorder: ${e.localizedMessage ?: "Unknown error"}")
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("AudioRecord failed to initialize. Another app may hold exclusive audio focus.")
            return
        }

        audioRecord = record
        try {
            record.startRecording()
        } catch (e: Exception) {
            record.release()
            audioRecord = null
            onError("Could not start audio recording: ${e.localizedMessage ?: "Unknown error"}")
            return
        }

        captureJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ByteArray(CHUNK_BYTES)
            while (isActive) {
                val bytesRead = record.read(readBuffer, 0, readBuffer.size)
                if (bytesRead > 0) {
                    val chunk = readBuffer.copyOf(bytesRead)
                    val rms = calculateNormalizedRms(chunk)
                    onAudioChunk(chunk, rms)
                } else if (bytesRead < 0) {
                    onError("Audio capture interrupted (code $bytesRead).")
                    break
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun buildPlaybackAudioRecord(
        mediaProjection: MediaProjection,
        bufferSize: Int
    ): AudioRecord {
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AUDIO_FORMAT)
            .setSampleRate(SAMPLE_RATE_HZ)
            .setChannelMask(CHANNEL_CONFIG)
            .build()

        return AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(playbackConfig)
            .build()
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        val record = audioRecord
        audioRecord = null
        if (record != null) {
            runCatching {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            }
            runCatching { record.release() }
        }
    }

    private fun calculateNormalizedRms(pcmBytes: ByteArray): Float {
        if (pcmBytes.size < 2) return 0f
        var sumSquares = 0.0
        val sampleCount = pcmBytes.size / 2
        val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            val sample = bb.short.toDouble()
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / sampleCount)
        // Normalize against speech dynamic range (~12000 out of 32768)
        return (rms / 9000.0).toFloat().coerceIn(0f, 1f)
    }
}

object WavEncoder {
    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = AudioCaptureManager.SAMPLE_RATE_HZ,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val totalDataLen = dataSize + 36

        val out = ByteArrayOutputStream(44 + dataSize)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat = 1 (PCM)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)
        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }
}
