package com.example.service

import com.example.domain.model.AudioSourceMode
import com.example.domain.model.LiveSubtitleState
import com.example.domain.model.TranslatorStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TranslatorStateHolder {

    private val _state = MutableStateFlow(LiveSubtitleState())
    val state: StateFlow<LiveSubtitleState> = _state.asStateFlow()

    fun setOverlayVisible(visible: Boolean) {
        _state.update { it.copy(isOverlayVisible = visible) }
    }

    fun setOverlayMinimized(minimized: Boolean) {
        _state.update { it.copy(isOverlayMinimized = minimized) }
    }

    fun setCapturing(capturing: Boolean, sourceMode: AudioSourceMode) {
        _state.update {
            it.copy(
                isCapturing = capturing,
                activeSource = sourceMode,
                errorMessage = if (capturing) null else it.errorMessage
            )
        }
    }

    fun updateStatus(status: TranslatorStatus, message: String) {
        _state.update {
            it.copy(
                status = status,
                statusMessage = message,
                errorMessage = if (status != TranslatorStatus.ERROR) null else it.errorMessage
            )
        }
    }

    fun setError(message: String) {
        _state.update {
            it.copy(
                status = TranslatorStatus.ERROR,
                statusMessage = "Error",
                errorMessage = message,
                isCapturing = false,
                audioLevel = 0f
            )
        }
    }

    fun updatePartialSubtitle(text: String) {
        _state.update {
            it.copy(
                currentSubtitle = text,
                errorMessage = null
            )
        }
    }

    fun commitCompletedSubtitle(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        _state.update { current ->
            val updatedHistory = (current.recentLines + cleaned).takeLast(12)
            current.copy(
                currentSubtitle = "",
                recentLines = updatedHistory,
                errorMessage = null
            )
        }
    }

    fun clearSubtitles() {
        _state.update {
            it.copy(
                currentSubtitle = "",
                recentLines = emptyList(),
                errorMessage = null
            )
        }
    }

    fun updateAudioLevel(level: Float, incrementPacket: Boolean = false) {
        _state.update {
            it.copy(
                audioLevel = level,
                packetsSent = if (incrementPacket) it.packetsSent + 1 else it.packetsSent
            )
        }
    }

    fun resetToIdle(keepOverlayVisible: Boolean) {
        _state.update {
            it.copy(
                status = TranslatorStatus.IDLE,
                statusMessage = "Paused",
                isCapturing = false,
                audioLevel = 0f,
                isOverlayVisible = keepOverlayVisible
            )
        }
    }
}
