package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ApiKeyTestResult
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioSourceMode
import com.example.domain.model.SubtitleStyleMode
import com.example.domain.model.TranslationEngineMode
import com.example.ui.components.OverlayAppearancePreviewCard
import com.example.ui.theme.AmberBurmese
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NotoSansMyanmarFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasApiKey: Boolean,
    maskedApiKey: String,
    apiKeyTestResult: ApiKeyTestResult,
    onSaveApiKey: (String) -> Unit,
    onTestApiKey: (String?) -> Unit,
    onClearApiKey: () -> Unit,
    onSelectAudioSource: (AudioSourceMode) -> Unit,
    onSelectEngineMode: (TranslationEngineMode) -> Unit,
    onSelectLiveModel: (String) -> Unit,
    onSelectRestModel: (String) -> Unit,
    onSelectSubtitleStyle: (SubtitleStyleMode) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyDraft by remember { mutableStateOf("") }
    var showKeyPlaintext by remember { mutableStateOf(false) }
    var buildGuideExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            // 1. Gemini API Key Vault
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = CyanElectric,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_api_section),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        if (hasApiKey) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = EmeraldContainer,
                                border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = maskedApiKey.ifBlank { "Saved" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLive,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        label = { Text(stringResource(R.string.settings_api_hint)) },
                        placeholder = { Text("AIzaSy...", color = TextMuted) },
                        singleLine = true,
                        visualTransformation = if (showKeyPlaintext) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showKeyPlaintext = !showKeyPlaintext },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (showKeyPlaintext) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = "Toggle API key visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanElectric,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = CyanElectric,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (apiKeyDraft.isNotBlank()) {
                                    onSaveApiKey(apiKeyDraft)
                                    apiKeyDraft = ""
                                }
                            },
                            enabled = apiKeyDraft.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_api_key_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanElectric,
                                contentColor = CyanOnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_api_save), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onTestApiKey(apiKeyDraft.takeIf { it.isNotBlank() })
                            },
                            enabled = apiKeyDraft.isNotBlank() || hasApiKey,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_api_key_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AmberBurmese.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberBurmese),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            if (apiKeyTestResult is ApiKeyTestResult.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AmberBurmese
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.NetworkCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_api_test), fontWeight = FontWeight.SemiBold)
                        }

                        if (hasApiKey) {
                            OutlinedButton(
                                onClick = onClearApiKey,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("clear_api_key_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.settings_api_clear),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Key Test Result Feedback
                    when (apiKeyTestResult) {
                        is ApiKeyTestResult.Success -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldContainer,
                                border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldLive,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Verified (${apiKeyTestResult.latencyMs}ms) • ${apiKeyTestResult.modelCount} models",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                    }
                                    if (apiKeyTestResult.liveModels.isNotEmpty()) {
                                        Text(
                                            text = "Live WebSocket models detected (tap to select):",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            apiKeyTestResult.liveModels.take(4).forEach { model ->
                                                FilterChip(
                                                    selected = settings.liveModelId == model,
                                                    onClick = { onSelectLiveModel(model) },
                                                    label = {
                                                        Text(
                                                            text = model,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = CyanElectric.copy(alpha = 0.22f),
                                                        selectedLabelColor = CyanElectric
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Key valid! Use 'Gemini Flash Audio Stream' engine for universal REST compatibility.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AmberBurmese
                                        )
                                    }
                                }
                            }
                        }

                        is ApiKeyTestResult.Error -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CoralContainer,
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = apiKeyTestResult.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        else -> Unit
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_api_security_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 2. Audio Input Source & Translation Engine
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_source_section),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    val isMediaSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    AudioSourceMode.entries.forEach { mode ->
                        val enabled = mode != AudioSourceMode.INTERNAL_MEDIA || isMediaSupported
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) { onSelectAudioSource(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.audioSource == mode,
                                onClick = { if (enabled) onSelectAudioSource(mode) },
                                enabled = enabled,
                                colors = RadioButtonDefaults.colors(selectedColor = CyanElectric)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (enabled) TextPrimary else TextMuted
                                )
                                Text(
                                    text = when (mode) {
                                        AudioSourceMode.INTERNAL_MEDIA ->
                                            if (isMediaSupported) {
                                                "Captures audio playing inside YouTube, browsers, or video players"
                                            } else {
                                                "Requires Android 10 (API 29) or higher"
                                            }
                                        AudioSourceMode.MICROPHONE ->
                                            "Captures spoken voice or external speaker audio via device mic"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_engine_section),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    TranslationEngineMode.entries.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectEngineMode(engine) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.engineMode == engine,
                                onClick = { onSelectEngineMode(engine) },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanElectric)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = engine.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = engine.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    val activeModelList = if (settings.engineMode == TranslationEngineMode.LIVE_WEBSOCKET) {
                        AppSettings.PRESET_LIVE_MODELS
                    } else {
                        AppSettings.PRESET_REST_MODELS
                    }
                    val activeModel = if (settings.engineMode == TranslationEngineMode.LIVE_WEBSOCKET) {
                        settings.liveModelId
                    } else {
                        settings.restModelId
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeModelList.forEach { model ->
                            FilterChip(
                                selected = activeModel == model,
                                onClick = {
                                    if (settings.engineMode == TranslationEngineMode.LIVE_WEBSOCKET) {
                                        onSelectLiveModel(model)
                                    } else {
                                        onSelectRestModel(model)
                                    }
                                },
                                label = {
                                    Text(
                                        text = model,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanElectric.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanElectric
                                )
                            )
                        }
                    }
                }
            }

            // 3. Target Language (Fixed Burmese) & Subtitle Style
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = AmberBurmese,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_target_section),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, AmberBurmese.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Burmese • မြန်မာဘာသာ (Fixed)",
                                fontFamily = NotoSansMyanmarFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = AmberBurmese
                            )
                            Text(
                                text = "Noto Sans Myanmar",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    SubtitleStyleMode.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSubtitleStyle(style) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.subtitleStyle == style,
                                onClick = { onSelectSubtitleStyle(style) },
                                colors = RadioButtonDefaults.colors(selectedColor = AmberBurmese)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = style.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Floating Overlay Appearance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = CyanElectric,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_overlay_section),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    OverlayAppearancePreviewCard(settings = settings)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Burmese Font Size", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text("${settings.overlayFontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelMedium, color = CyanElectric)
                        }
                        Slider(
                            value = settings.overlayFontSizeSp,
                            onValueChange = onFontSizeChange,
                            valueRange = 14f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanElectric,
                                activeTrackColor = CyanElectric
                            ),
                            modifier = Modifier.testTag("font_size_slider")
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Background Opacity", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text("${(settings.overlayOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = CyanElectric)
                        }
                        Slider(
                            value = settings.overlayOpacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0.50f..0.96f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanElectric,
                                activeTrackColor = CyanElectric
                            ),
                            modifier = Modifier.testTag("overlay_opacity_slider")
                        )
                    }
                }
            }

            // 5. Build & APK Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { buildGuideExpanded = !buildGuideExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                tint = AmberBurmese,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_build_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (buildGuideExpanded) {
                                Icons.Rounded.ExpandLess
                            } else {
                                Icons.Rounded.ExpandMore
                            },
                            contentDescription = "Toggle APK build guide",
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = buildGuideExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Android Studio (Debug / Release APK)",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanElectric
                            )
                            Text(
                                text = "1. Open project in Android Studio Ladybug+ (JDK 17).\n" +
                                    "2. Build > Build Bundle(s) / APK(s) > Build APK(s).\n" +
                                    "3. Or run CLI: ./gradlew assembleDebug\n" +
                                    "   Output: app/build/outputs/apk/debug/app-debug.apk",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "GitHub Actions CI (.github/workflows/android-apk.yml)",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberBurmese
                            )
                            Text(
                                text = "Push to main or trigger workflow_dispatch in GitHub Actions to automatically build and upload app-debug.apk as a downloadable artifact. See BUILD_INSTRUCTIONS.md in repo root for signing release APKs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
