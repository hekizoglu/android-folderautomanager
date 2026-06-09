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

// ── Light Palette (Turkuaz / Teal) ────────────────────────────────────────────

private val Teal600               = Color(0xFF00897B) // Light primary
private val Teal100               = Color(0xFFB2DFDB) // Light primaryContainer
private val Cyan400               = Color(0xFF26C6DA) // Secondary
private val LightBackground       = Color(0xFFFAFAFA)
private val LightSurface          = Color(0xFFFFFFFF)
private val LightSurfaceVariant   = Color(0xFFF1F3F4)
private val LightOnBackground     = Color(0xFF202124)
private val LightOnSurface        = Color(0xFF202124)
private val LightOnSurfaceVariant = Color(0xFF5F6368)

// ── Dark Palette (Turkuaz / Teal) ─────────────────────────────────────────────

private val Teal300               = Color(0xFF4DB6AC) // Dark primary
private val Teal700               = Color(0xFF00695C) // Dark primaryContainer
private val DarkBackground        = Color(0xFF202124)
private val DarkSurface           = Color(0xFF2D2D2D)
private val DarkSurfaceVariant    = Color(0xFF3C3C3C)
private val DarkOnBackground      = Color(0xFFE8EAED)
private val DarkOnSurface         = Color(0xFFE8EAED)
private val DarkOnSurfaceVariant  = Color(0xFF9AA0A6)

private val ErrorColor            = Color(0xFFCF6679)

// ── Color Schemes ─────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = Teal600,
    onPrimary            = Color.White,
    primaryContainer     = Teal100,
    onPrimaryContainer   = Teal700,
    secondary            = Cyan400,
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
    primary              = Teal300,
    onPrimary            = Color(0xFF003731),
    primaryContainer     = Teal700,
    onPrimaryContainer   = Teal100,
    secondary            = Cyan400,
    onSecondary          = Color(0xFF00363D),
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
