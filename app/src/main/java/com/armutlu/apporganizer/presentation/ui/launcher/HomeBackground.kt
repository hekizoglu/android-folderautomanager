package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Ana ekran arka plan stilleri (tur 14'e kadar HomeScreen.kt içinde yaşardı; legacy
 * HomeScreen silinince kalıcı evine taşındı).
 */
private fun homeBackgroundBrush(style: String): Brush = when (style) {
    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_GECE_MAVISI ->
        Brush.verticalGradient(listOf(Color(0xFF0A1128), Color(0xFF1B2A4A)))
    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_MINIMAL_GRI ->
        Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF2E2E2E)))
    else -> // HOME_BG_TURKUAZ + bilinmeyen deger fallback
        Brush.verticalGradient(listOf(Color(0xFF00897B), Color(0xFF26C6DA)))
}

/**
 * Ortak kök zemin modifier'ı — HomeV2 ve FolderScreen aynı mantığı paylaşır.
 * "Duvar Kağıdı" seçiliyken TRANSPARAN kalınır (windowShowWallpaper=true ile sistem
 * duvar kağıdı görünür); diğer stillerde (turkuaz/gece mavisi/minimal gri/düz renk)
 * OPAK boyanır — duvar kağıdı hiç sızmaz. FolderScreen'den çıkışta flaş olmaması
 * için klasör ekranı da aynı fonksiyonu kullanır.
 */
internal fun Modifier.homeRootBackground(
    bgType: String,
    bgColorInt: Int,
    bgGradientStyle: String,
): Modifier = when (bgType) {
    "wallpaper" -> this // transparan — sistem duvar kağıdı sızar
    "solid", "wallpaper_color" -> this.background(Color(bgColorInt))
    else -> this.background(homeBackgroundBrush(bgGradientStyle)) // "gradient" + bilinmeyen fallback
}
