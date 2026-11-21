package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.TranslatorApplication
import com.example.audio.AudioCaptureManager
import com.example.data.remote.GeminiLiveTranslatorClient
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioSourceMode
import com.example.domain.model.TranslatorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TranslatorOverlayService : Service() {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_START_TRANSLATION = "com.example.action.START_TRANSLATION"
        const val ACTION_STOP_TRANSLATION = "com.example.action.STOP_TRANSLATION"
        const val ACTION_HIDE_OVERLAY = "com.example.action.HIDE_OVERLAY"
        const val ACTION_CLEAR_SUBTITLES = "com.example.action.CLEAR_SUBTITLES"
        const val ACTION_REFRESH_APPEARANCE = "com.example.action.REFRESH_APPEARANCE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_SOURCE_OVERRIDE = "extra_source_override"

        private const val CHANNEL_ID = "live_subtitle_overlay_channel"
        private const val NOTIFICATION_ID = 4201

        fun createShowOverlayIntent(context: Context): Intent =
            Intent(context, TranslatorOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }

        fun createStartMicIntent(context: Context): Intent =
            Intent(context, TranslatorOverlayService::class.java).apply {
                action = ACTION_START_TRANSLATION
                putExtra(EXTRA_SOURCE_OVERRIDE, AudioSourceMode.MICROPHONE.name)
            }

        fun createStartMediaProjectionIntent(
            context: Context,
            resultCode: Int,
            data: Intent
        ): Intent = Intent(context, TranslatorOverlayService::class.java).apply {
            action = ACTION_START_TRANSLATION
            putExtra(EXTRA_SOURCE_OVERRIDE, AudioSourceMode.INTERNAL_MEDIA.name)
            putExtra(EXTRA_RESULT_CODE, resultCode)
            putExtra(EXTRA_RESULT_DATA, data)
        }

        fun createStopTranslationIntent(context: Context): Intent =
            Intent(context, TranslatorOverlayService::class.java).apply {
                action = ACTION_STOP_TRANSLATION
            }

        fun createHideOverlayIntent(context: Context): Intent =
            Intent(context, TranslatorOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }

        fun createClearIntent(context: Context): Intent =
            Intent(context, TranslatorOverlayService::class.java).apply {
                action = ACTION_CLEAR_SUBTITLES
            }

        fun createRefreshAppearanceIntent(context: Context): Intent =
            Intent(context, TranslatorOverlayService::class.java).apply {
                action = ACTION_REFRESH_APPEARANCE
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val app: TranslatorApplication
        get() = application as TranslatorApplication

    private val audioCaptureManager = AudioCaptureManager()
    private val geminiClient = GeminiLiveTranslatorClient()
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var overlayController: OverlayWindowController? = null
    private var currentSettings = AppSettings()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        overlayController = OverlayWindowController(
            context = this,
            onTogglePlayStop = { handleOverlayTogglePlayStop() },
            onClearSubtitles = { app.stateHolder.clearSubtitles() },
            onToggleMinimize = {
                val current = app.stateHolder.state.value.isOverlayMinimized
                app.stateHolder.setOverlayMinimized(!current)
            },
            onCloseOverlay = { stopAllAndSelf() },
            onBoundsChanged = { x, y, w, h ->
                serviceScope.launch {
                    app.settingsRepository.saveOverlayBounds(x, y, w, h)
                }
            }
        )

        serviceScope.launch {
            app.settingsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
                overlayController?.updateAppearance(settings)
            }
        }

        serviceScope.launch {
            app.stateHolder.state.collect { state ->
                if (state.isOverlayVisible && Settings.canDrawOverlays(this@TranslatorOverlayService)) {
                    overlayController?.updateState(state)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                promoteForeground(AudioSourceMode.MICROPHONE)
                showOverlayIfPermitted()
            }

            ACTION_START_TRANSLATION -> {
                val overrideName = intent.getStringExtra(EXTRA_SOURCE_OVERRIDE)
                val source = overrideName
                    ?.let { runCatching { AudioSourceMode.valueOf(it) }.getOrNull() }
                    ?: currentSettings.audioSource
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                startTranslationPipeline(source, resultCode, resultData)
            }

            ACTION_STOP_TRANSLATION -> {
                stopTranslationOnly(keepOverlay = overlayController?.isAttached == true)
            }

            ACTION_HIDE_OVERLAY -> {
                stopAllAndSelf()
            }

            ACTION_CLEAR_SUBTITLES -> {
                app.stateHolder.clearSubtitles()
            }

            ACTION_REFRESH_APPEARANCE -> {
                serviceScope.launch {
                    val latest = app.settingsRepository.settingsFlow.first()
                    currentSettings = latest
                    overlayController?.updateAppearance(latest)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun handleOverlayTogglePlayStop() {
        val state = app.stateHolder.state.value
        if (state.isCapturing || state.status == TranslatorStatus.CONNECTING) {
            stopTranslationOnly(keepOverlay = true)
        } else {
            serviceScope.launch {
                val settings = app.settingsRepository.settingsFlow.first()
                if (settings.audioSource == AudioSourceMode.INTERNAL_MEDIA) {
                    // MediaProjection consent must originate from an Activity
                    val launchIntent = Intent(this@TranslatorOverlayService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(MainActivity.EXTRA_TRIGGER_MEDIA_PROJECTION, true)
                    }
                    startActivity(launchIntent)
                } else {
                    startTranslationPipeline(AudioSourceMode.MICROPHONE, Activity.RESULT_CANCELED, null)
                }
            }
        }
    }

    private fun startTranslationPipeline(
        source: AudioSourceMode,
        resultCode: Int,
        resultData: Intent?
    ) {
        // Must promote foreground service BEFORE acquiring MediaProjection on Android 14+
        promoteForeground(source)

        serviceScope.launch {
            val settings = app.settingsRepository.settingsFlow.first()
            currentSettings = settings

            if (Settings.canDrawOverlays(this@TranslatorOverlayService)) {
                showOverlayIfPermitted()
            }

            val apiKey = app.settingsRepository.getApiKey()
            if (apiKey.isNullOrBlank()) {
                app.stateHolder.setError(getString(R.string.err_missing_api_key))
                return@launch
            }

            stopCaptureAndClientOnly()

            var projection: MediaProjection? = null
            if (source == AudioSourceMode.INTERNAL_MEDIA) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    app.stateHolder.setError(getString(R.string.err_media_projection_unsupported))
                    return@launch
                }
                if (resultCode != Activity.RESULT_OK || resultData == null) {
                    app.stateHolder.setError(getString(R.string.err_media_projection_denied))
                    return@launch
                }
                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projection = runCatching { mgr.getMediaProjection(resultCode, resultData) }.getOrNull()
                if (projection == null) {
                    app.stateHolder.setError("Could not acquire MediaProjection session.")
                    return@launch
                }
                val cb = object : MediaProjection.Callback() {
                    override fun onStop() {
                        serviceScope.launch {
                            stopTranslationOnly(keepOverlay = overlayController?.isAttached == true)
                        }
                    }
                }
                projection.registerCallback(cb, null)
                projectionCallback = cb
                mediaProjection = projection
            }

            app.stateHolder.setCapturing(true, source)
            app.stateHolder.updateStatus(TranslatorStatus.CONNECTING, "Connecting to Gemini…")

            geminiClient.start(
                scope = serviceScope,
                apiKey = apiKey,
                settings = settings.copy(audioSource = source),
                listener = object : GeminiLiveTranslatorClient.Listener {
                    override fun onStatus(status: TranslatorStatus, message: String) {
                        app.stateHolder.updateStatus(status, message)
                    }

                    override fun onPartialSubtitle(burmeseText: String) {
                        app.stateHolder.updatePartialSubtitle(burmeseText)
                    }

                    override fun onCompletedSubtitle(burmeseText: String) {
                        app.stateHolder.commitCompletedSubtitle(burmeseText)
                        serviceScope.launch(Dispatchers.IO) {
                            app.transcriptRepository.addSegment(
                                burmeseText = burmeseText,
                                sourceMode = source,
                                engineMode = settings.engineMode
                            )
                        }
                    }

                    override fun onError(errorMessage: String) {
                        app.stateHolder.setError(errorMessage)
                        stopCaptureAndClientOnly()
                    }
                }
            )

            audioCaptureManager.startCapture(
                scope = serviceScope,
                sourceMode = source,
                mediaProjection = projection,
                onAudioChunk = { pcmBytes, rms ->
                    geminiClient.sendPcmAudio(pcmBytes, rms)
                    app.stateHolder.updateAudioLevel(rms, incrementPacket = true)
                },
                onError = { err ->
                    app.stateHolder.setError(err)
                    stopCaptureAndClientOnly()
                }
            )
        }
    }

    private fun showOverlayIfPermitted() {
        if (!Settings.canDrawOverlays(this)) {
            app.stateHolder.setError(getString(R.string.err_overlay_permission))
            return
        }
        app.stateHolder.setOverlayVisible(true)
        overlayController?.show(currentSettings, app.stateHolder.state.value)
    }

    private fun stopCaptureAndClientOnly() {
        audioCaptureManager.stopCapture()
        geminiClient.stop()
        val proj = mediaProjection
        mediaProjection = null
        val cb = projectionCallback
        projectionCallback = null
        if (proj != null) {
            if (cb != null) runCatching { proj.unregisterCallback(cb) }
            runCatching { proj.stop() }
        }
    }

    private fun stopTranslationOnly(keepOverlay: Boolean) {
        stopCaptureAndClientOnly()
        app.stateHolder.resetToIdle(keepOverlayVisible = keepOverlay)
        if (!keepOverlay) {
            stopAllAndSelf()
        }
    }

    private fun stopAllAndSelf() {
        stopCaptureAndClientOnly()
        overlayController?.hide()
        app.stateHolder.resetToIdle(keepOverlayVisible = false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteForeground(source: AudioSourceMode) {
        val notification = buildNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (source == AudioSourceMode.INTERNAL_MEDIA) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
        } else {
            0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            createHideOverlayIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_body_listening))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCaptureAndClientOnly()
        overlayController?.hide()
        serviceScope.cancel()
        super.onDestroy()
    }
}
