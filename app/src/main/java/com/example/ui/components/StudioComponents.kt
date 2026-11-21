package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppSettings
import com.example.domain.model.TranslatorStatus
import com.example.ui.theme.AmberBurmese
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NotoSansMyanmarFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun StatusPillBadge(
    status: TranslatorStatus,
    label: String,
    modifier: Modifier = Modifier
) {
    val targetColor = when (status) {
        TranslatorStatus.LISTENING -> EmeraldLive
        TranslatorStatus.TRANSLATING -> CyanElectric
        TranslatorStatus.CONNECTING -> AmberBurmese
        TranslatorStatus.ERROR -> CoralError
        TranslatorStatus.IDLE -> TextMuted
    }
    val dotColor by animateColorAsState(targetValue = targetColor, label = "status_dot")

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val isLive = status == TranslatorStatus.LISTENING ||
        status == TranslatorStatus.TRANSLATING ||
        status == TranslatorStatus.CONNECTING

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = dotColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isLive) pulseAlpha else 1f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LiveWaveformVisualizer(
    audioLevel: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val smoothedLevel by animateFloatAsState(
        targetValue = if (isActive) audioLevel.coerceIn(0.08f, 1f) else 0.04f,
        animationSpec = spring(stiffness = 380f),
        label = "audio_rms"
    )
    val infinite = rememberInfiniteTransition(label = "wave_phase")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_val"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val barCount = 28
        val totalWidth = size.width
        val gap = 4.dp.toPx()
        val barWidth = ((totalWidth - gap * (barCount - 1)) / barCount).coerceAtLeast(3f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normPos = i.toFloat() / barCount
            val envelope = sin(normPos * Math.PI).toFloat()
            val waveMod = if (isActive) {
                (0.55f + 0.45f * sin(phase + i * 0.55f))
            } else {
                0.3f
            }
            val barHeight = (size.height * smoothedLevel * envelope * waveMod)
                .coerceIn(4.dp.toPx(), size.height)
            val x = i * (barWidth + gap)
            val y = centerY - barHeight / 2f

            val brush = Brush.verticalGradient(
                colors = if (isActive) {
                    listOf(CyanElectric, AmberBurmese.copy(alpha = 0.85f))
                } else {
                    listOf(ObsidianOutline, ObsidianOutline.copy(alpha = 0.5f))
                }
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun ChecklistItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isReady: Boolean,
    actionLabel: String,
    testTag: String,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isReady) EmeraldLive.copy(alpha = 0.16f) else AmberBurmese.copy(alpha = 0.16f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isReady) EmeraldLive else AmberBurmese,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isReady) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "$title ready",
                tint = EmeraldLive,
                modifier = Modifier.size(24.dp)
            )
        } else {
            OutlinedButton(
                onClick = onActionClick,
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag),
                border = BorderStroke(1.dp, AmberBurmese.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberBurmese)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun OverlayAppearancePreviewCard(
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianBg.copy(alpha = settings.overlayOpacity),
        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.45f)),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldLive)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE • ${settings.audioSource.shortBadge}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "${settings.overlayFontSizeSp.toInt()}sp • ${(settings.overlayOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanElectric
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "မင်္ဂလာပါ — တိုက်ရိုက် မြန်မာစာတန်းထိုး နမူနာပုံစံ ဖြစ်ပါသည်။",
                fontFamily = NotoSansMyanmarFontFamily,
                fontSize = settings.overlayFontSizeSp.sp,
                lineHeight = (settings.overlayFontSizeSp * 1.45f).sp,
                color = Color.White
            )
        }
    }
}
