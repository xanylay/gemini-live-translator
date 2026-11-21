package com.example.domain.model

enum class AudioSourceMode(val displayName: String, val shortBadge: String) {
    INTERNAL_MEDIA("Internal / Media Audio", "MEDIA"),
    MICROPHONE("Microphone", "MIC")
}

enum class TranslationEngineMode(val displayName: String, val subtitle: String) {
    LIVE_WEBSOCKET(
        displayName = "Gemini Live WebSocket",
        subtitle = "Ultra-low latency continuous streaming (BidiGenerateContent)"
    ),
    CHUNKED_REST(
        displayName = "Gemini Flash Audio Stream",
        subtitle = "VAD speech-segmented chunks via REST (High compatibility)"
    )
}

enum class SubtitleStyleMode(val displayName: String, val description: String) {
    CONCISE_SUBTITLES(
        displayName = "Concise Subtitles",
        description = "Short, fast-reading Burmese captions ideal for videos"
    ),
    FULL_SENTENCES(
        displayName = "Full Natural Burmese",
        description = "Complete conversational sentences with full context"
    )
}

enum class TranslatorStatus {
    IDLE,
    CONNECTING,
    LISTENING,
    TRANSLATING,
    ERROR
}

sealed interface ApiKeyTestResult {
    data object Idle : ApiKeyTestResult
    data object Testing : ApiKeyTestResult
    data class Success(
        val modelCount: Int,
        val liveModels: List<String>,
        val latencyMs: Long
    ) : ApiKeyTestResult
    data class Error(val message: String) : ApiKeyTestResult
}

data class AppSettings(
    val audioSource: AudioSourceMode = AudioSourceMode.MICROPHONE,
    val engineMode: TranslationEngineMode = TranslationEngineMode.LIVE_WEBSOCKET,
    val liveModelId: String = DEFAULT_LIVE_MODEL,
    val restModelId: String = DEFAULT_REST_MODEL,
    val subtitleStyle: SubtitleStyleMode = SubtitleStyleMode.CONCISE_SUBTITLES,
    val overlayFontSizeSp: Float = 18f,
    val overlayOpacity: Float = 0.88f,
    val overlayWidthDp: Int = 330,
    val overlayHeightDp: Int = 156,
    val overlayX: Int = 36,
    val overlayY: Int = 180,
) {
    companion object {
        const val DEFAULT_LIVE_MODEL = "gemini-2.0-flash-live-001"
        const val DEFAULT_REST_MODEL = "gemini-2.5-flash"

        val PRESET_LIVE_MODELS = listOf(
            "gemini-2.0-flash-live-001",
            "gemini-2.5-flash-native-audio-preview-09-2025",
            "gemini-live-2.5-flash-preview"
        )

        val PRESET_REST_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash"
        )
    }
}

data class LiveSubtitleState(
    val status: TranslatorStatus = TranslatorStatus.IDLE,
    val statusMessage: String = "Ready to translate",
    val errorMessage: String? = null,
    val currentSubtitle: String = "",
    val recentLines: List<String> = emptyList(),
    val audioLevel: Float = 0f,
    val isOverlayVisible: Boolean = false,
    val isOverlayMinimized: Boolean = false,
    val activeSource: AudioSourceMode = AudioSourceMode.MICROPHONE,
    val packetsSent: Long = 0L,
    val isCapturing: Boolean = false,
)
