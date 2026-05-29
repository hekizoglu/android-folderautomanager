package com.armutlu.apporganizer.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary   = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer   = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary   = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer   = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFFFBFE),
    surface    = Color(0xFFFFFBFE),
    surfaceVariant   = Color(0xFFE7E0EC),
    onBackground = Color(0xFF1C1B1F),
    onSurface    = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    error   = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer   = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary   = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer   = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary   = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer   = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF1C1B1F),
    surface    = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFF49454F),
    onBackground = Color(0xFFE6E1E5),
    onSurface    = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error   = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer   = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun AppOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
