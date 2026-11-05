package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkM3Colors = darkColorScheme(
    primary = DarkAppColors.accent,
    onPrimary = DarkAppColors.accentButtonText,
    primaryContainer = DarkAppColors.accentSoft,
    onPrimaryContainer = DarkAppColors.accent,
    secondary = DarkAppColors.accent,
    onSecondary = DarkAppColors.accentButtonText,
    secondaryContainer = DarkAppColors.accentSoft,
    onSecondaryContainer = DarkAppColors.accent,
    background = DarkAppColors.background,
    onBackground = DarkAppColors.text,
    surface = DarkAppColors.surface,
    onSurface = DarkAppColors.text,
    surfaceVariant = DarkAppColors.sheet,
    onSurfaceVariant = DarkAppColors.textMuted,
    outline = DarkAppColors.border,
    outlineVariant = DarkAppColors.divider,
    error = DarkAppColors.dangerRed,
    onError = DarkAppColors.background,
    errorContainer = DarkAppColors.redSoft,
    onErrorContainer = DarkAppColors.dangerRed
)

private val LightM3Colors = lightColorScheme(
    primary = LightAppColors.accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightAppColors.accentSoft,
    onPrimaryContainer = LightAppColors.accent,
    secondary = LightAppColors.accent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightAppColors.accentSoft,
    onSecondaryContainer = LightAppColors.accent,
    background = LightAppColors.background,
    onBackground = LightAppColors.text,
    surface = LightAppColors.surface,
    onSurface = LightAppColors.text,
    surfaceVariant = LightAppColors.sheet,
    onSurfaceVariant = LightAppColors.textMuted,
    outline = LightAppColors.border,
    outlineVariant = LightAppColors.divider,
    error = LightAppColors.dangerRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightAppColors.redSoft,
    onErrorContainer = LightAppColors.dangerRed
)

@Composable
fun PartnerLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val m3Colors = if (darkTheme) DarkM3Colors else LightM3Colors
    val appTypography = AppTypographyTokens()

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = Typography,
            content = content
        )
    }
}
