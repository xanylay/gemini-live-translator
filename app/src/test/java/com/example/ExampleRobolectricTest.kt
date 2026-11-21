package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.WavEncoder
import com.example.data.local.TranscriptEntity
import com.example.data.local.TranscriptRepository
import com.example.data.local.TranscriptDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gemini Live Translator", appName)
    }

    @Test
    fun `wav encoder writes valid RIFF WAVE header`() {
        val pcm = ByteArray(320) { 1 }
        val wav = WavEncoder.pcmToWav(pcm)
        assertEquals(364, wav.size)
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(wav, 12, 4, Charsets.US_ASCII))
        assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
    }

    @Test
    fun `transcript repository formats Burmese txt export`() {
        val fakeDao = object : TranscriptDao {
            override fun observeAllNewestFirst(): Flow<List<TranscriptEntity>> = flowOf(emptyList())
            override suspend fun getAllChronological(): List<TranscriptEntity> = emptyList()
            override suspend fun insert(entry: TranscriptEntity): Long = 1L
            override suspend fun deleteById(id: Long) {}
            override suspend fun deleteAll() {}
        }
        val repo = TranscriptRepository(fakeDao)
        val txt = repo.formatEntriesAsTxt(
            listOf(
                TranscriptEntity(
                    id = 1,
                    burmeseText = "မင်္ဂလာပါ ခင်ဗျာ",
                    timestampMillis = 1700000000000L,
                    sourceMode = "MIC",
                    engineMode = "LIVE_WEBSOCKET"
                )
            )
        )
        assertTrue(txt.contains("Gemini Live Translator - Burmese Subtitle Transcript"))
        assertTrue(txt.contains("မင်္ဂလာပါ ခင်ဗျာ"))
        assertTrue(txt.contains("(MIC)"))
    }
}
