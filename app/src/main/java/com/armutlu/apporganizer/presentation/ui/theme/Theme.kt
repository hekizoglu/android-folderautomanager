package com.armutlu.apporganizer.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

private val ErrorColor = Color(0xFFCF6679)

private fun buildColorScheme(
    theme: AppTheme,
    darkTheme: Boolean,
): androidx.compose.material3.ColorScheme {
    val onContent = when (theme) {
        AppTheme.IOS   -> Color(0xFFF2F2F7)
        AppTheme.AMOLED -> Color(0xFFFFFFFF)
        else           -> Color(0xFFE8EAED)
    }
    val onVariant = when (theme) {
        AppTheme.IOS   -> Color(0xFF8E8E93)
        AppTheme.AMOLED -> Color(0xFF9E9E9E)
        else           -> Color(0xFF9AA0A6)
    }
    val outline = when (theme) {
        AppTheme.IOS   -> Color(0xFF3A3A3C)
        AppTheme.AMOLED -> Color(0xFF1A1A1A)
        else           -> Color(0xFF444444)
    }
    return if (darkTheme) {
        darkColorScheme(
            primary              = theme.primary,
            onPrimary            = Color.White,
            primaryContainer     = theme.primary.copy(alpha = 0.25f),
            onPrimaryContainer   = theme.primary,
            secondary            = theme.secondary,
            onSecondary          = Color.White,
            secondaryContainer   = theme.secondary.copy(alpha = 0.18f),
            onSecondaryContainer = theme.secondary,
            background           = theme.background,
            onBackground         = onContent,
            surface              = theme.surface,
            onSurface            = onContent,
            surfaceVariant       = theme.surface.copy(alpha = 0.8f),
            onSurfaceVariant     = onVariant,
            outline              = outline,
            outlineVariant       = outline.copy(alpha = 0.7f),
            error                = ErrorColor,
            onError              = Color.White,
        )
    } else {
        lightColorScheme(
            primary              = theme.primary,
            onPrimary            = Color.White,
            primaryContainer     = theme.primary.copy(alpha = 0.12f),
            onPrimaryContainer   = theme.primary,
            secondary            = theme.secondary,
            onSecondary          = Color.White,
            secondaryContainer   = theme.secondary.copy(alpha = 0.10f),
            onSecondaryContainer = theme.secondary,
            background           = Color(0xFFF8F9FB),
            onBackground         = Color(0xFF1B1C1F),
            surface              = Color.White,
            onSurface            = Color(0xFF1B1C1F),
            surfaceVariant       = Color(0xFFF1F3F4),
            onSurfaceVariant     = Color(0xFF5F6368),
            outline              = theme.primary.copy(alpha = 0.35f),
            outlineVariant       = theme.primary.copy(alpha = 0.18f),
            error                = Color(0xFFB3261E),
            onError              = Color.White,
        )
    }
}

private fun buildTypography(font: AppFont): Typography {
    val family = when (font) {
        AppFont.ROUNDED -> FontFamily.SansSerif
        AppFont.MONO    -> FontFamily.Monospace
        AppFont.SERIF   -> FontFamily.Serif
        else            -> FontFamily.Default
    }
    val base = Typography()
    return Typography(
        displayLarge   = base.displayLarge.copy(fontFamily = family),
        displayMedium  = base.displayMedium.copy(fontFamily = family),
        displaySmall   = base.displaySmall.copy(fontFamily = family),
        headlineLarge  = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall  = base.headlineSmall.copy(fontFamily = family),
        titleLarge     = base.titleLarge.copy(fontFamily = family),
        titleMedium    = base.titleMedium.copy(fontFamily = family),
        titleSmall     = base.titleSmall.copy(fontFamily = family),
        bodyLarge      = base.bodyLarge.copy(fontFamily = family),
        bodyMedium     = base.bodyMedium.copy(fontFamily = family),
        bodySmall      = base.bodySmall.copy(fontFamily = family),
        labelLarge     = base.labelLarge.copy(fontFamily = family),
        labelMedium    = base.labelMedium.copy(fontFamily = family),
        labelSmall     = base.labelSmall.copy(fontFamily = family),
    )
}

@Composable
fun AppOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    val currentTheme by themePrefs.themeFlow.collectAsState(initial = AppTheme.default())
    val currentFont by themePrefs.fontFlow.collectAsState(initial = AppFont.DEFAULT)

    // Material You — Android 12+ dinamik renk şeması, duvar kağıdına uyum sağlar
    val colorScheme = if (
        currentTheme == AppTheme.DYNAMIC &&
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        buildColorScheme(currentTheme, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = buildTypography(currentFont),
        content     = content,
    )
}
