package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.utils.AppPrefs

/**
 * Ana ekranın cam yüzeyleri ve klasör ekranı için tek renk kaynağı.
 * Klasör zemini her zaman opaktır; açık/özel renkler okunabilirlik için otomatik koyulaştırılır.
 */
internal data class HomeObjectPalette(
    val surfaceTint: Color,
    val borderTint: Color,
    val folderBackground: Color,
)

internal val LocalHomeObjectPalette = staticCompositionLocalOf {
    HomeObjectPalette(
        surfaceTint = Color.White,
        borderTint = Color.White,
        folderBackground = Color(0xFF0B0D10),
    )
}

/**
 * İnce uyumluluk katmanı — gerçek doğruluk kaynağı [AppPrefs]'teki
 * `getHomeObjectColorMode`/`setHomeObjectColorMode`/`getHomeObjectCustomColor`/
 * `setHomeObjectCustomColor` API'sidir (backup export/import zincirine bağlı
 * olabilmesi için). Anahtar adları ve varsayılanlar AppPrefs ile birebir aynı.
 */
internal object HomeObjectStylePrefs {
    const val KEY_MODE = AppPrefs.KEY_HOME_OBJECT_COLOR_MODE
    const val KEY_CUSTOM_COLOR = AppPrefs.KEY_HOME_OBJECT_CUSTOM_COLOR

    const val MODE_AUTO = AppPrefs.HOME_OBJECT_COLOR_MODE_AUTO
    const val MODE_DARK = AppPrefs.HOME_OBJECT_COLOR_MODE_DARK
    const val MODE_LIGHT = AppPrefs.HOME_OBJECT_COLOR_MODE_LIGHT
    const val MODE_CUSTOM = AppPrefs.HOME_OBJECT_COLOR_MODE_CUSTOM

    fun getMode(context: Context): String = AppPrefs.getHomeObjectColorMode(context)

    fun setMode(context: Context, mode: String) = AppPrefs.setHomeObjectColorMode(context, mode)

    fun getCustomColor(context: Context): Int = AppPrefs.getHomeObjectCustomColor(context)

    fun setCustomColor(context: Context, color: Int) = AppPrefs.setHomeObjectCustomColor(context, color)
}

@Composable
internal fun rememberHomeObjectPalette(): HomeObjectPalette {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val watchedKeys = setOf(
            HomeObjectStylePrefs.KEY_MODE,
            HomeObjectStylePrefs.KEY_CUSTOM_COLOR,
            AppPrefs.KEY_BG_TYPE,
            AppPrefs.KEY_BG_COLOR,
            AppPrefs.KEY_HOME_BACKGROUND_STYLE,
        )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in watchedKeys) revision++
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return remember(revision) { resolveHomeObjectPalette(context) }
}

internal fun resolveHomeObjectPalette(context: Context): HomeObjectPalette {
    val mode = HomeObjectStylePrefs.getMode(context)
    val backgroundBase = resolveBackgroundBaseColor(context)

    return when (mode) {
        HomeObjectStylePrefs.MODE_DARK -> HomeObjectPalette(
            surfaceTint = Color(0xFF111820),
            borderTint = Color(0xFFD5DEE8),
            folderBackground = Color(0xFF0B0D10),
        )

        HomeObjectStylePrefs.MODE_LIGHT -> HomeObjectPalette(
            surfaceTint = Color.White,
            borderTint = Color.White,
            // Klasör içindeki mevcut beyaz metin/ikon kontrastını korumak için açık seçimde
            // yalnız cam yüzeyler açılır; tam ekran klasör zemini güvenli koyu nötr kalır.
            folderBackground = Color(0xFF242A31),
        )

        HomeObjectStylePrefs.MODE_CUSTOM -> {
            val custom = Color(HomeObjectStylePrefs.getCustomColor(context))
            HomeObjectPalette(
                surfaceTint = custom,
                borderTint = mix(custom, Color.White, 0.52f),
                folderBackground = readableDarkTone(custom),
            )
        }

        else -> {
            val surface = mix(backgroundBase, Color.White, 0.18f)
            HomeObjectPalette(
                surfaceTint = surface,
                borderTint = mix(backgroundBase, Color.White, 0.58f),
                folderBackground = readableDarkTone(backgroundBase),
            )
        }
    }
}

private fun resolveBackgroundBaseColor(context: Context): Color = when (AppPrefs.getBgType(context)) {
    "solid", "wallpaper_color" -> Color(AppPrefs.getBgColor(context))
    "gradient" -> when (AppPrefs.getHomeBackgroundStyle(context)) {
        AppPrefs.HOME_BG_GECE_MAVISI -> Color(0xFF13203A)
        AppPrefs.HOME_BG_MINIMAL_GRI -> Color(0xFF252525)
        else -> Color(0xFF0D918A)
    }
    // Sistem duvar kağıdının dominant rengini her açılışta analiz etmek pahalı ve OEM'e göre
    // tutarsızdır; otomatik modda güvenli koyu nötr kullanılır.
    else -> Color(0xFF182126)
}

private fun readableDarkTone(color: Color): Color {
    val factor = when {
        color.luminance() >= 0.55f -> 0.22f
        color.luminance() >= 0.25f -> 0.34f
        color.luminance() >= 0.10f -> 0.48f
        else -> 0.68f
    }
    return Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = 1f,
    )
}

private fun mix(from: Color, to: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = 1f,
    )
}
