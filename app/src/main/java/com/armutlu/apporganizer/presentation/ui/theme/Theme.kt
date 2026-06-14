package com.armutlu.apporganizer.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map

private val ErrorColor = Color(0xFFCF6679)

private fun buildColorScheme(theme: AppTheme) = darkColorScheme(
    primary              = theme.primary,
    onPrimary            = Color.White,
    primaryContainer     = theme.primary.copy(alpha = 0.3f),
    onPrimaryContainer   = theme.primary,
    secondary            = theme.secondary,
    onSecondary          = Color.White,
    secondaryContainer   = theme.secondary.copy(alpha = 0.2f),
    onSecondaryContainer = theme.secondary,
    background           = theme.background,
    onBackground         = Color(0xFFE8EAED),
    surface              = theme.surface,
    onSurface            = Color(0xFFE8EAED),
    surfaceVariant       = theme.surface.copy(alpha = 0.8f),
    onSurfaceVariant     = Color(0xFF9AA0A6),
    outline              = Color(0xFF444444),
    outlineVariant       = Color(0xFF333333),
    error                = ErrorColor,
    onError              = Color.White,
)

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

private val THEME_KEY = stringPreferencesKey("app_theme")
private val FONT_KEY  = stringPreferencesKey("app_font")

@Composable
fun AppOrganizerTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentTheme by context.themeDataStore.data
        .map { prefs -> AppTheme.fromName(prefs[THEME_KEY] ?: AppTheme.TEAL.name) }
        .collectAsState(initial = AppTheme.TEAL)
    val currentFont by context.themeDataStore.data
        .map { prefs -> AppFont.fromName(prefs[FONT_KEY] ?: AppFont.DEFAULT.name) }
        .collectAsState(initial = AppFont.DEFAULT)

    MaterialTheme(
        colorScheme = buildColorScheme(currentTheme),
        typography  = buildTypography(currentFont),
        content     = content,
    )
}
