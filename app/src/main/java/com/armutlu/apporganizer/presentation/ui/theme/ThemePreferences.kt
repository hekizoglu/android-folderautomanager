package com.armutlu.apporganizer.presentation.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.armutlu.apporganizer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

enum class AppTheme(
    val label: String,
    val labelRes: Int,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color
) {
    /** Material You — duvar kağıdından türetilen dinamik renkler (Android 12+). Renkler fallback (API <31 = TEAL görünümü). */
    DYNAMIC(
        label = "Dinamik (Material You)",
        labelRes = R.string.theme_dynamic,
        primary = Color(0xFF00897B),
        secondary = Color(0xFF26C6DA),
        background = Color(0xFF1A1A2E),
        surface = Color(0xFF16213E)
    ),
    TEAL(
        label = "Turkuaz",
        labelRes = R.string.theme_teal,
        primary = Color(0xFF00897B),
        secondary = Color(0xFF26C6DA),
        background = Color(0xFF1A1A2E),
        surface = Color(0xFF16213E)
    ),
    PURPLE(
        label = "Mor",
        labelRes = R.string.theme_purple,
        primary = Color(0xFF7B1FA2),
        secondary = Color(0xFFCE93D8),
        background = Color(0xFF1A1025),
        surface = Color(0xFF231535)
    ),
    OCEAN(
        label = "Okyanus",
        labelRes = R.string.theme_ocean,
        primary = Color(0xFF1565C0),
        secondary = Color(0xFF42A5F5),
        background = Color(0xFF0A1628),
        surface = Color(0xFF0D2137)
    ),
    SUNSET(
        label = "Gün Batımı",
        labelRes = R.string.theme_sunset,
        primary = Color(0xFFE64A19),
        secondary = Color(0xFFFFB74D),
        background = Color(0xFF1C1008),
        surface = Color(0xFF2A1800)
    ),
    MONO(
        label = "Mono",
        labelRes = R.string.theme_mono,
        primary = Color(0xFF607D8B),
        secondary = Color(0xFF90A4AE),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E)
    ),
    IOS(
        label = "iOS",
        labelRes = R.string.theme_ios,
        primary = Color(0xFF007AFF),
        secondary = Color(0xFF5AC8FA),
        background = Color(0xFF1C1C1E),
        surface = Color(0xFF2C2C2E)
    ),
    AMOLED(
        label = "AMOLED",
        labelRes = R.string.theme_amoled,
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF69FF47),
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A)
    );

    val previewBrush: Brush get() = Brush.linearGradient(listOf(primary, secondary))

    companion object {
        fun fromName(name: String): AppTheme =
            entries.firstOrNull { it.name == name } ?: default()

        /** Varsayılan tema: Android 12+ → Material You dinamik, altı → Turkuaz. */
        fun default(): AppTheme =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) DYNAMIC else TEAL
    }
}

enum class AppFont(val label: String, val labelRes: Int) {
    DEFAULT("Sistem", R.string.font_default),
    ROUNDED("Yuvarlatılmış", R.string.font_rounded),
    MONO("Mono", R.string.font_mono),
    SERIF("Serif", R.string.font_serif);

    companion object {
        fun fromName(name: String): AppFont =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val FONT_KEY  = stringPreferencesKey("app_font")

    val themeFlow: Flow<AppTheme> = context.themeDataStore.data
        .map { prefs -> AppTheme.fromName(prefs[THEME_KEY] ?: AppTheme.default().name) }

    val fontFlow: Flow<AppFont> = context.themeDataStore.data
        .map { prefs -> AppFont.fromName(prefs[FONT_KEY] ?: AppFont.DEFAULT.name) }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setFont(font: AppFont) {
        context.themeDataStore.edit { it[FONT_KEY] = font.name }
    }
}
