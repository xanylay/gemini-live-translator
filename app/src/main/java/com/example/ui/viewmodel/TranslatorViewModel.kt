package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsRepository
import com.example.data.local.TranscriptEntity
import com.example.data.local.TranscriptRepository
import com.example.data.remote.GeminiApiTester
import com.example.domain.model.ApiKeyTestResult
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioSourceMode
import com.example.domain.model.LiveSubtitleState
import com.example.domain.model.SubtitleStyleMode
import com.example.domain.model.TranslationEngineMode
import com.example.service.TranslatorStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionSnapshot(
    val canDrawOverlays: Boolean = false,
    val hasRecordAudio: Boolean = false,
    val hasNotificationPermission: Boolean = true,
)

class TranslatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val transcriptRepository: TranscriptRepository,
    private val stateHolder: TranslatorStateHolder,
    private val apiTester: GeminiApiTester
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val hasApiKey: StateFlow<Boolean> = settingsRepository.hasApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val maskedApiKey: StateFlow<String> = settingsRepository.maskedApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val liveState: StateFlow<LiveSubtitleState> = stateHolder.state

    val transcripts: StateFlow<List<TranscriptEntity>> = transcriptRepository.observeTranscripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _apiKeyTestResult = MutableStateFlow<ApiKeyTestResult>(ApiKeyTestResult.Idle)
    val apiKeyTestResult: StateFlow<ApiKeyTestResult> = _apiKeyTestResult.asStateFlow()

    private val _permissions = MutableStateFlow(PermissionSnapshot())
    val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    fun updatePermissions(snapshot: PermissionSnapshot) {
        _permissions.value = snapshot
    }

    fun saveApiKey(rawKey: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(rawKey)
            _apiKeyTestResult.value = ApiKeyTestResult.Idle
            onSaved()
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
            _apiKeyTestResult.value = ApiKeyTestResult.Idle
        }
    }

    fun testApiKey(candidateKey: String?) {
        viewModelScope.launch {
            _apiKeyTestResult.value = ApiKeyTestResult.Testing
            val keyToTest = candidateKey?.trim()?.takeIf { it.isNotEmpty() }
                ?: settingsRepository.getApiKey().orEmpty()
            val result = apiTester.testApiKey(keyToTest)
            _apiKeyTestResult.value = result
        }
    }

    fun setAudioSource(mode: AudioSourceMode) {
        viewModelScope.launch { settingsRepository.setAudioSource(mode) }
    }

    fun setEngineMode(mode: TranslationEngineMode) {
        viewModelScope.launch { settingsRepository.setEngineMode(mode) }
    }

    fun setLiveModelId(modelId: String) {
        viewModelScope.launch { settingsRepository.setLiveModelId(modelId) }
    }

    fun setRestModelId(modelId: String) {
        viewModelScope.launch { settingsRepository.setRestModelId(modelId) }
    }

    fun setSubtitleStyle(style: SubtitleStyleMode) {
        viewModelScope.launch { settingsRepository.setSubtitleStyle(style) }
    }

    fun setOverlayFontSize(sizeSp: Float) {
        viewModelScope.launch { settingsRepository.setOverlayFontSize(sizeSp) }
    }

    fun setOverlayOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.setOverlayOpacity(opacity) }
    }

    fun clearLiveSubtitles() {
        stateHolder.clearSubtitles()
    }

    fun showError(message: String) {
        stateHolder.setError(message)
    }

    fun deleteTranscript(id: Long) {
        viewModelScope.launch { transcriptRepository.deleteSegment(id) }
    }

    fun clearAllTranscripts() {
        viewModelScope.launch { transcriptRepository.clearAll() }
    }

    suspend fun exportTranscriptTxt(): String {
        return transcriptRepository.exportChronologicalTxt()
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val transcriptRepository: TranscriptRepository,
        private val stateHolder: TranslatorStateHolder,
        private val apiTester: GeminiApiTester
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TranslatorViewModel(
                settingsRepository = settingsRepository,
                transcriptRepository = transcriptRepository,
                stateHolder = stateHolder,
                apiTester = apiTester
            ) as T
        }
    }
}
