package com.example.data.local

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioSourceMode
import com.example.domain.model.SubtitleStyleMode
import com.example.domain.model.TranslationEngineMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gemini_translator_prefs"
)

class SettingsRepository(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) {
    private object Keys {
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_gemini_api_key")
        val AUDIO_SOURCE = stringPreferencesKey("audio_source_mode")
        val ENGINE_MODE = stringPreferencesKey("translation_engine_mode")
        val LIVE_MODEL_ID = stringPreferencesKey("live_model_id")
        val REST_MODEL_ID = stringPreferencesKey("rest_model_id")
        val SUBTITLE_STYLE = stringPreferencesKey("subtitle_style_mode")
        val OVERLAY_FONT_SIZE = floatPreferencesKey("overlay_font_size_sp")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_WIDTH_DP = intPreferencesKey("overlay_width_dp")
        val OVERLAY_HEIGHT_DP = intPreferencesKey("overlay_height_dp")
        val OVERLAY_X = intPreferencesKey("overlay_x")
        val OVERLAY_Y = intPreferencesKey("overlay_y")
    }

    private val defaultAudioSource: AudioSourceMode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AudioSourceMode.INTERNAL_MEDIA
        } else {
            AudioSourceMode.MICROPHONE
        }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            audioSource = prefs[Keys.AUDIO_SOURCE]
                ?.let { runCatching { AudioSourceMode.valueOf(it) }.getOrNull() }
                ?: defaultAudioSource,
            engineMode = prefs[Keys.ENGINE_MODE]
                ?.let { runCatching { TranslationEngineMode.valueOf(it) }.getOrNull() }
                ?: TranslationEngineMode.LIVE_WEBSOCKET,
            liveModelId = prefs[Keys.LIVE_MODEL_ID]?.takeIf { it.isNotBlank() }
                ?: AppSettings.DEFAULT_LIVE_MODEL,
            restModelId = prefs[Keys.REST_MODEL_ID]?.takeIf { it.isNotBlank() }
                ?: AppSettings.DEFAULT_REST_MODEL,
            subtitleStyle = prefs[Keys.SUBTITLE_STYLE]
                ?.let { runCatching { SubtitleStyleMode.valueOf(it) }.getOrNull() }
                ?: SubtitleStyleMode.CONCISE_SUBTITLES,
            overlayFontSizeSp = prefs[Keys.OVERLAY_FONT_SIZE] ?: 18f,
            overlayOpacity = prefs[Keys.OVERLAY_OPACITY] ?: 0.88f,
            overlayWidthDp = prefs[Keys.OVERLAY_WIDTH_DP] ?: 330,
            overlayHeightDp = prefs[Keys.OVERLAY_HEIGHT_DP] ?: 156,
            overlayX = prefs[Keys.OVERLAY_X] ?: 36,
            overlayY = prefs[Keys.OVERLAY_Y] ?: 180,
        )
    }

    val hasApiKeyFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        !prefs[Keys.ENCRYPTED_API_KEY].isNullOrBlank()
    }

    val maskedApiKeyFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        val encrypted = prefs[Keys.ENCRYPTED_API_KEY] ?: return@map ""
        val plain = cryptoManager.decrypt(encrypted) ?: return@map ""
        if (plain.length <= 8) {
            "••••••••"
        } else {
            "${plain.take(4)}••••••••${plain.takeLast(4)}"
        }
    }

    suspend fun getApiKey(): String? {
        val prefs = context.settingsDataStore.data.first()
        val encrypted = prefs[Keys.ENCRYPTED_API_KEY] ?: return null
        return cryptoManager.decrypt(encrypted)?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun saveApiKey(rawKey: String) {
        val cleaned = rawKey.trim()
        if (cleaned.isEmpty()) {
            clearApiKey()
            return
        }
        val encrypted = cryptoManager.encrypt(cleaned)
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_API_KEY] = encrypted
        }
    }

    suspend fun clearApiKey() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(Keys.ENCRYPTED_API_KEY)
        }
    }

    suspend fun setAudioSource(mode: AudioSourceMode) {
        context.settingsDataStore.edit { it[Keys.AUDIO_SOURCE] = mode.name }
    }

    suspend fun setEngineMode(mode: TranslationEngineMode) {
        context.settingsDataStore.edit { it[Keys.ENGINE_MODE] = mode.name }
    }

    suspend fun setLiveModelId(modelId: String) {
        context.settingsDataStore.edit { it[Keys.LIVE_MODEL_ID] = modelId.trim() }
    }

    suspend fun setRestModelId(modelId: String) {
        context.settingsDataStore.edit { it[Keys.REST_MODEL_ID] = modelId.trim() }
    }

    suspend fun setSubtitleStyle(style: SubtitleStyleMode) {
        context.settingsDataStore.edit { it[Keys.SUBTITLE_STYLE] = style.name }
    }

    suspend fun setOverlayFontSize(sizeSp: Float) {
        context.settingsDataStore.edit { it[Keys.OVERLAY_FONT_SIZE] = sizeSp.coerceIn(14f, 30f) }
    }

    suspend fun setOverlayOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[Keys.OVERLAY_OPACITY] = opacity.coerceIn(0.45f, 0.96f) }
    }

    suspend fun saveOverlayBounds(x: Int, y: Int, widthDp: Int, heightDp: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.OVERLAY_X] = x
            prefs[Keys.OVERLAY_Y] = y
            prefs[Keys.OVERLAY_WIDTH_DP] = widthDp.coerceIn(220, 620)
            prefs[Keys.OVERLAY_HEIGHT_DP] = heightDp.coerceIn(96, 460)
        }
    }
}
