// FILE 1: Theme.kt
package com.armutlu.apporganizer.presentation.ui.theme

// Pixel Launcher / Material You exact colors:
// Light scheme: white surface, Google Blue primary (#1A73E8), clean
// Dark scheme: #202124 background (Google dark), #8AB4F8 primary (Google blue light)
// The launcher itself is transparent (wallpaper shows),
// but the management screens (AppListScreen, Settings) use these colors.

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light Palette (Google / Pixel Launcher) ───────────────────────────────────

private val GoogleBlue            = Color(0xFF1A73E8)
private val GoogleBlueLightMode   = Color(0xFF8AB4F8)
private val LightPrimaryContainer = Color(0xFFD3E3FD)
private val LightBackground       = Color(0xFFFAFAFA)
private val LightSurface          = Color(0xFFFFFFFF)
private val LightSurfaceVariant   = Color(0xFFF1F3F4) // Google's light grey
private val LightOnBackground     = Color(0xFF202124) // Google dark text
private val LightOnSurface        = Color(0xFF202124)
private val LightOnSurfaceVariant = Color(0xFF5F6368) // Google secondary text

// ── Dark Palette (Google / Pixel Launcher) ────────────────────────────────────

private val DarkBackground        = Color(0xFF202124) // Google dark
private val DarkSurface           = Color(0xFF2D2D2D)
private val DarkSurfaceVariant    = Color(0xFF3C3C3C)
private val DarkOnBackground      = Color(0xFFE8EAED)
private val DarkOnSurface         = Color(0xFFE8EAED)
private val DarkOnSurfaceVariant  = Color(0xFF9AA0A6)

private val ErrorColor            = Color(0xFFCF6679)

// ── Color Schemes ─────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = GoogleBlue,
    onPrimary            = Color.White,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = GoogleBlue,
    secondary            = GoogleBlue,
    onSecondary          = Color.White,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    error                = ErrorColor,
    onError              = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary              = GoogleBlueLightMode,
    onPrimary            = Color(0xFF002884),
    primaryContainer     = Color(0xFF0040CB),
    onPrimaryContainer   = LightPrimaryContainer,
    secondary            = GoogleBlueLightMode,
    onSecondary          = Color(0xFF002884),
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    error                = ErrorColor,
    onError              = Color.White,
)

// ── Theme Composable ──────────────────────────────────────────────────────────

@Composable
fun AppOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = androidx.compose.material3.Typography(),
        content     = content,
    )
}
