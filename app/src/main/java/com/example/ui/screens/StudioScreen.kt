package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioSourceMode
import com.example.domain.model.LiveSubtitleState
import com.example.domain.model.TranslatorStatus
import com.example.ui.components.ChecklistItemRow
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AmberBurmese
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanOnPrimary
import com.example.ui.theme.NotoSansMyanmarFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioScreen(
    settings: AppSettings,
    liveState: LiveSubtitleState,
    hasApiKey: Boolean,
    canDrawOverlays: Boolean,
    hasMicPermission: Boolean,
    onSelectAudioSource: (AudioSourceMode) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartTranslation: () -> Unit,
    onStopTranslation: () -> Unit,
    onToggleOverlayOnly: () -> Unit,
    onClearLiveSubtitles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = liveState.isCapturing ||
        liveState.status == TranslatorStatus.CONNECTING ||
        liveState.status == TranslatorStatus.LISTENING ||
        liveState.status == TranslatorStatus.TRANSLATING

    val isMediaSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

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
            // Hero Studio Card with generated waveform banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner_1795237722397),
                        contentDescription = stringResource(R.string.studio_hero_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.25f),
                                        ObsidianBg.copy(alpha = 0.72f),
                                        ObsidianSurface
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPillBadge(
                                status = liveState.status,
                                label = liveState.statusMessage
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = AmberBurmese.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, AmberBurmese.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Translate,
                                        contentDescription = null,
                                        tint = AmberBurmese,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "မြန်မာ (MM)",
                                        fontFamily = NotoSansMyanmarFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AmberBurmese
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.studio_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${settings.engineMode.displayName} • ${settings.audioSource.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        LiveWaveformVisualizer(
                            audioLevel = liveState.audioLevel,
                            isActive = isLive
                        )
                    }
                }
            }

            // Error Banner (if any)
            AnimatedVisibility(visible = !liveState.errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CoralContainer,
                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Error",
                            tint = CoralError,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = liveState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Quick Audio Source Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_source_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AudioSourceCard(
                        title = stringResource(R.string.studio_source_media),
                        subtitle = if (isMediaSupported) "Videos & apps" else "Needs Android 10+",
                        icon = Icons.Rounded.AudioFile,
                        selected = settings.audioSource == AudioSourceMode.INTERNAL_MEDIA,
                        enabled = isMediaSupported,
                        testTag = "source_internal_media",
                        onClick = { onSelectAudioSource(AudioSourceMode.INTERNAL_MEDIA) },
                        modifier = Modifier.weight(1f)
                    )
                    AudioSourceCard(
                        title = stringResource(R.string.studio_source_mic),
                        subtitle = "Voice & speaker",
                        icon = Icons.Rounded.Mic,
                        selected = settings.audioSource == AudioSourceMode.MICROPHONE,
                        enabled = true,
                        testTag = "source_microphone",
                        onClick = { onSelectAudioSource(AudioSourceMode.MICROPHONE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary Launch & Overlay Controls
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (isLive) onStopTranslation() else onStartTranslation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_stop_translation_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) CoralError else CyanElectric,
                        contentColor = if (isLive) Color.White else CyanOnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = if (isLive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLive) {
                            stringResource(R.string.studio_stop_overlay)
                        } else {
                            stringResource(R.string.studio_start_overlay)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onToggleOverlayOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("toggle_overlay_visibility_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        imageVector = if (liveState.isOverlayVisible) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = null,
                        tint = CyanElectric,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (liveState.isOverlayVisible) {
                            stringResource(R.string.studio_hide_overlay_only)
                        } else {
                            stringResource(R.string.studio_open_overlay_only)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Live In-App Burmese Subtitle Monitor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                border = BorderStroke(1.dp, CyanElectric.copy(alpha = if (isLive) 0.45f else 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = null,
                                tint = CyanElectric,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.studio_live_monitor_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = onClearLiveSubtitles,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_live_monitor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = stringResource(R.string.studio_clear_live),
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val liveDisplay = buildString {
                        liveState.recentLines.takeLast(4).forEachIndexed { index, line ->
                            if (index > 0) append("\n")
                            append(line)
                        }
                        if (liveState.currentSubtitle.isNotBlank()) {
                            if (isNotEmpty()) append("\n")
                            append(liveState.currentSubtitle)
                        }
                    }

                    Text(
                        text = liveDisplay.ifBlank { stringResource(R.string.studio_live_placeholder) },
                        fontFamily = NotoSansMyanmarFontFamily,
                        fontSize = settings.overlayFontSizeSp.sp,
                        lineHeight = (settings.overlayFontSizeSp * 1.48f).sp,
                        color = if (liveDisplay.isBlank()) TextMuted else TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Readiness Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = AmberBurmese,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.studio_checklist_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ChecklistItemRow(
                        icon = Icons.Rounded.Key,
                        title = stringResource(R.string.perm_api_key_title),
                        subtitle = if (hasApiKey) {
                            stringResource(R.string.perm_api_key_ready)
                        } else {
                            stringResource(R.string.perm_api_key_missing)
                        },
                        isReady = hasApiKey,
                        actionLabel = stringResource(R.string.action_configure),
                        testTag = "checklist_api_key_button",
                        onActionClick = onOpenSettings
                    )

                    ChecklistItemRow(
                        icon = Icons.Rounded.Layers,
                        title = stringResource(R.string.perm_overlay_title),
                        subtitle = if (canDrawOverlays) {
                            stringResource(R.string.perm_overlay_granted)
                        } else {
                            stringResource(R.string.perm_overlay_needed)
                        },
                        isReady = canDrawOverlays,
                        actionLabel = stringResource(R.string.action_grant),
                        testTag = "checklist_overlay_button",
                        onActionClick = onRequestOverlayPermission
                    )

                    ChecklistItemRow(
                        icon = Icons.Rounded.Mic,
                        title = stringResource(R.string.perm_mic_title),
                        subtitle = if (hasMicPermission) {
                            stringResource(R.string.perm_mic_granted)
                        } else {
                            stringResource(R.string.perm_mic_needed)
                        },
                        isReady = hasMicPermission,
                        actionLabel = stringResource(R.string.action_grant),
                        testTag = "checklist_mic_button",
                        onActionClick = onRequestMicPermission
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AudioSourceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        selected -> CyanElectric
        else -> ObsidianOutline
    }
    val bgColor = when {
        selected -> CyanElectric.copy(alpha = 0.12f)
        else -> ObsidianSurface
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) CyanElectric else if (enabled) TextSecondary else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) TextPrimary else TextMuted,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) CyanElectric.copy(alpha = 0.9f) else TextSecondary,
                maxLines = 1
            )
        }
    }
}
