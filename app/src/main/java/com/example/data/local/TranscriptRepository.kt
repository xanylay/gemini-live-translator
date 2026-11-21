package com.example.data.local

import com.example.domain.model.AudioSourceMode
import com.example.domain.model.TranslationEngineMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class TranscriptRepository(private val dao: TranscriptDao) {

    fun observeTranscripts(): Flow<List<TranscriptEntity>> = dao.observeAllNewestFirst()

    suspend fun addSegment(
        burmeseText: String,
        sourceMode: AudioSourceMode,
        engineMode: TranslationEngineMode
    ) {
        val trimmed = burmeseText.trim()
        if (trimmed.isEmpty()) return
        dao.insert(
            TranscriptEntity(
                burmeseText = trimmed,
                timestampMillis = System.currentTimeMillis(),
                sourceMode = sourceMode.shortBadge,
                engineMode = engineMode.name
            )
        )
    }

    suspend fun deleteSegment(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun exportChronologicalTxt(): String {
        val entries = dao.getAllChronological()
        return formatEntriesAsTxt(entries)
    }

    fun formatEntriesAsTxt(entries: List<TranscriptEntity>): String {
        if (entries.isEmpty()) return ""
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("Gemini Live Translator - Burmese Subtitle Transcript")
        sb.appendLine("Exported: ${dateFormat.format(Date())}")
        sb.appendLine("====================================================")
        sb.appendLine()
        entries.forEach { entry ->
            val timeStr = dateFormat.format(Date(entry.timestampMillis))
            sb.appendLine("[$timeStr] (${entry.sourceMode})")
            sb.appendLine(entry.burmeseText)
            sb.appendLine()
        }
        return sb.toString()
    }
}
