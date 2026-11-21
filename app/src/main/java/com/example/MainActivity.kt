package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.AudioSourceMode
import com.example.service.TranslatorOverlayService
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.TranscriptsScreen
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanSoftContainer
import com.example.ui.theme.GeminiLiveTranslatorTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PermissionSnapshot
import com.example.ui.viewmodel.TranslatorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val ROUTE_STUDIO = "studio"
        const val ROUTE_TRANSCRIPTS = "transcripts"
        const val ROUTE_SETTINGS = "settings"
        const val EXTRA_TRIGGER_MEDIA_PROJECTION = "extra_trigger_media_projection"
    }

    private val viewModel: TranslatorViewModel by viewModels {
        val app = application as TranslatorApplication
        TranslatorViewModel.Factory(
            settingsRepository = app.settingsRepository,
            transcriptRepository = app.transcriptRepository,
            stateHolder = app.stateHolder,
            apiTester = app.apiTester
        )
    }

    private var triggerProjectionRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        refreshPermissionSnapshot()
        if (intent?.getBooleanExtra(EXTRA_TRIGGER_MEDIA_PROJECTION, false) == true) {
            triggerProjectionRequest = true
        }

        setContent {
            GeminiLiveTranslatorTheme(darkTheme = true) {
                TranslatorAppShell(
                    viewModel = viewModel,
                    shouldTriggerMediaProjection = triggerProjectionRequest,
                    onConsumedMediaProjectionTrigger = { triggerProjectionRequest = false },
                    onRefreshPermissions = { refreshPermissionSnapshot() },
                    onStartMicService = {
                        ContextCompat.startForegroundService(
                            this,
                            TranslatorOverlayService.createStartMicIntent(this)
                        )
                    },
                    onStartMediaProjectionService = { code, data ->
                        ContextCompat.startForegroundService(
                            this,
                            TranslatorOverlayService.createStartMediaProjectionIntent(this, code, data)
                        )
                    },
                    onStopServiceTranslation = {
                        startService(TranslatorOverlayService.createStopTranslationIntent(this))
                    },
                    onShowOverlayOnly = {
                        ContextCompat.startForegroundService(
                            this,
                            TranslatorOverlayService.createShowOverlayIntent(this)
                        )
                    },
                    onHideOverlay = {
                        startService(TranslatorOverlayService.createHideOverlayIntent(this))
                    },
                    onRefreshOverlayAppearance = {
                        if (viewModel.liveState.value.isOverlayVisible) {
                            startService(TranslatorOverlayService.createRefreshAppearanceIntent(this))
                        }
                    },
                    onCopyToClipboard = { text ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Burmese Transcript", text))
                    },
                    onShareText = { text ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Gemini Live Translator - Burmese Transcript")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Burmese Transcript"))
                    },
                    onWriteTextToUri = { uri, text ->
                        contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(text.toByteArray(Charsets.UTF_8))
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_TRIGGER_MEDIA_PROJECTION, false)) {
            triggerProjectionRequest = true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionSnapshot()
    }

    private fun refreshPermissionSnapshot() {
        val canDrawOverlays = Settings.canDrawOverlays(this)
        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.updatePermissions(
            PermissionSnapshot(
                canDrawOverlays = canDrawOverlays,
                hasRecordAudio = hasMic,
                hasNotificationPermission = hasNotifications
            )
        )
    }
}

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun TranslatorAppShell(
    viewModel: TranslatorViewModel,
    shouldTriggerMediaProjection: Boolean,
    onConsumedMediaProjectionTrigger: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onStartMicService: () -> Unit,
    onStartMediaProjectionService: (Int, Intent) -> Unit,
    onStopServiceTranslation: () -> Unit,
    onShowOverlayOnly: () -> Unit,
    onHideOverlay: () -> Unit,
    onRefreshOverlayAppearance: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onShareText: (String) -> Unit,
    onWriteTextToUri: (Uri, String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val maskedApiKey by viewModel.maskedApiKey.collectAsStateWithLifecycle()
    val apiKeyTestResult by viewModel.apiKeyTestResult.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val transcripts by viewModel.transcripts.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()

    var pendingStartAfterMic by remember { mutableStateOf(false) }

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshPermissions()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            onStartMediaProjectionService(result.resultCode, data)
        } else {
            val msg = context.getString(R.string.err_media_projection_denied)
            viewModel.showError(msg)
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    fun launchMediaProjectionFlow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val msg = context.getString(R.string.err_media_projection_unsupported)
            viewModel.showError(msg)
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            return
        }
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        if (granted) {
            if (pendingStartAfterMic) {
                pendingStartAfterMic = false
                if (settings.audioSource == AudioSourceMode.INTERNAL_MEDIA) {
                    launchMediaProjectionFlow()
                } else {
                    onStartMicService()
                }
            }
        } else {
            pendingStartAfterMic = false
            val msg = context.getString(R.string.err_mic_permission)
            viewModel.showError(msg)
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val txt = viewModel.exportTranscriptTxt()
                runCatching { onWriteTextToUri(uri, txt) }
                    .onSuccess { snackbarHostState.showSnackbar("Saved Burmese transcript .txt file") }
                    .onFailure { snackbarHostState.showSnackbar("Failed to save file: ${it.localizedMessage}") }
            }
        }
    }

    fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        overlaySettingsLauncher.launch(intent)
    }

    fun handleStartTranslation() {
        if (!hasApiKey) {
            val msg = context.getString(R.string.err_missing_api_key)
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            navController.navigate(MainActivity.ROUTE_SETTINGS) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            return
        }

        if (!Settings.canDrawOverlays(context)) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.err_overlay_permission))
            }
            openOverlayPermissionSettings()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!micGranted) {
            pendingStartAfterMic = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (settings.audioSource == AudioSourceMode.INTERNAL_MEDIA) {
            launchMediaProjectionFlow()
        } else {
            onStartMicService()
        }
    }

    LaunchedEffect(shouldTriggerMediaProjection) {
        if (shouldTriggerMediaProjection) {
            onConsumedMediaProjectionTrigger()
            launchMediaProjectionFlow()
        }
    }

    val destinations = remember {
        listOf(
            NavDestination(
                route = MainActivity.ROUTE_STUDIO,
                labelRes = R.string.nav_studio,
                selectedIcon = Icons.Rounded.GraphicEq,
                unselectedIcon = Icons.Outlined.GraphicEq,
                testTag = "nav_studio"
            ),
            NavDestination(
                route = MainActivity.ROUTE_TRANSCRIPTS,
                labelRes = R.string.nav_transcripts,
                selectedIcon = Icons.Rounded.Description,
                unselectedIcon = Icons.Outlined.Description,
                testTag = "nav_transcripts"
            ),
            NavDestination(
                route = MainActivity.ROUTE_SETTINGS,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MainActivity.ROUTE_STUDIO

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanElectric,
                                    selectedTextColor = CyanElectric,
                                    indicatorColor = CyanSoftContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanElectric,
                                    selectedTextColor = CyanElectric,
                                    indicatorColor = CyanSoftContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = MainActivity.ROUTE_STUDIO,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(MainActivity.ROUTE_STUDIO) {
                        StudioScreen(
                            settings = settings,
                            liveState = liveState,
                            hasApiKey = hasApiKey,
                            canDrawOverlays = permissions.canDrawOverlays,
                            hasMicPermission = permissions.hasRecordAudio,
                            onSelectAudioSource = { viewModel.setAudioSource(it) },
                            onRequestOverlayPermission = { openOverlayPermissionSettings() },
                            onRequestMicPermission = {
                                pendingStartAfterMic = false
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onOpenSettings = { navigateTo(MainActivity.ROUTE_SETTINGS) },
                            onStartTranslation = { handleStartTranslation() },
                            onStopTranslation = { onStopServiceTranslation() },
                            onToggleOverlayOnly = {
                                if (!Settings.canDrawOverlays(context)) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.err_overlay_permission)
                                        )
                                    }
                                    openOverlayPermissionSettings()
                                } else if (liveState.isOverlayVisible) {
                                    onHideOverlay()
                                } else {
                                    onShowOverlayOnly()
                                }
                            },
                            onClearLiveSubtitles = { viewModel.clearLiveSubtitles() }
                        )
                    }

                    composable(MainActivity.ROUTE_TRANSCRIPTS) {
                        TranscriptsScreen(
                            transcripts = transcripts,
                            onExportTxt = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportTxtLauncher.launch("burmese_subtitles_$stamp.txt")
                            },
                            onShareTranscripts = {
                                coroutineScope.launch {
                                    val txt = viewModel.exportTranscriptTxt()
                                    if (txt.isNotBlank()) onShareText(txt)
                                }
                            },
                            onCopyAll = {
                                coroutineScope.launch {
                                    val txt = viewModel.exportTranscriptTxt()
                                    if (txt.isNotBlank()) {
                                        onCopyToClipboard(txt)
                                        snackbarHostState.showSnackbar("Copied all Burmese transcripts")
                                    }
                                }
                            },
                            onCopySingle = { line ->
                                onCopyToClipboard(line)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Copied Burmese subtitle")
                                }
                            },
                            onDeleteSingle = { id -> viewModel.deleteTranscript(id) },
                            onClearAll = {
                                viewModel.clearAllTranscripts()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Transcript history cleared")
                                }
                            }
                        )
                    }

                    composable(MainActivity.ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            hasApiKey = hasApiKey,
                            maskedApiKey = maskedApiKey,
                            apiKeyTestResult = apiKeyTestResult,
                            onSaveApiKey = { raw ->
                                viewModel.saveApiKey(raw) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("API key encrypted and saved locally")
                                    }
                                }
                            },
                            onTestApiKey = { candidate -> viewModel.testApiKey(candidate) },
                            onClearApiKey = {
                                viewModel.clearApiKey()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved API key removed")
                                }
                            },
                            onSelectAudioSource = { viewModel.setAudioSource(it) },
                            onSelectEngineMode = { viewModel.setEngineMode(it) },
                            onSelectLiveModel = { viewModel.setLiveModelId(it) },
                            onSelectRestModel = { viewModel.setRestModelId(it) },
                            onSelectSubtitleStyle = { viewModel.setSubtitleStyle(it) },
                            onFontSizeChange = {
                                viewModel.setOverlayFontSize(it)
                                onRefreshOverlayAppearance()
                            },
                            onOpacityChange = {
                                viewModel.setOverlayOpacity(it)
                                onRefreshOverlayAppearance()
                            }
                        )
                    }
                }
            }
        }
    }
}
