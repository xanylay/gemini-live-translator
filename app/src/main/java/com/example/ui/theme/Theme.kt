package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanSoftContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AmberBurmese,
    onSecondary = ObsidianBg,
    secondaryContainer = AmberSoftContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = IndigoAccent,
    onTertiary = ObsidianBg,
    tertiaryContainer = IndigoSoftContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = CoralError,
    onError = ObsidianBg,
    errorContainer = CoralContainer,
    onErrorContainer = TextPrimary,
)

private val StudioLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    primaryContainer = ColorTokens.LightCyanContainer,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = ColorTokens.LightAmberContainer,
    onSecondaryContainer = LightTextPrimary,
    tertiary = IndigoAccent,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = ColorTokens.LightOutline,
    error = CoralError,
)

private object ColorTokens {
    val LightCyanContainer = androidx.compose.ui.graphics.Color(0xFFE0F2FE)
    val LightAmberContainer = androidx.compose.ui.graphics.Color(0xFFFEF3C7)
    val LightOutline = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
}

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GeminiLiveTranslatorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
