package com.example

import android.app.Application
import com.example.data.local.CryptoManager
import com.example.data.local.SettingsRepository
import com.example.data.local.TranscriptDatabase
import com.example.data.local.TranscriptRepository
import com.example.data.remote.GeminiApiTester
import com.example.service.TranslatorStateHolder

class TranslatorApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var transcriptRepository: TranscriptRepository
        private set

    lateinit var stateHolder: TranslatorStateHolder
        private set

    lateinit var apiTester: GeminiApiTester
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this, CryptoManager())
        val db = TranscriptDatabase.getInstance(this)
        transcriptRepository = TranscriptRepository(db.transcriptDao())
        stateHolder = TranslatorStateHolder()
        apiTester = GeminiApiTester()
    }
}
