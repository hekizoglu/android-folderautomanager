package com.armutlu.apporganizer.presentation.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Android (Pixel) Görünümü" ayarı (KEY_PIXEL_LOOK_ENABLED, varsayılan KAPALI) açıkken
 * kullanılan sabit görsel kararlar — tek yerde toplanır, dağınık magic number bırakılmaz.
 *
 * Kırmızı çizgi: bu obje sadece SABİT DEĞER taşır, hiçbir composable'ı silmez/değiştirmez.
 * Tüketen composable'lar (FolderTile, AllAppsDrawer, PulseClockWidget) `if (pixelLook) ... else ...`
 * ile mevcut davranışı korur.
 */
object PixelLookPolicy {
    /** Stok Android klasör köşe yuvarlaklığı — squircle hissi (~%28 corner radius). */
    val FOLDER_CORNER_RADIUS_PERCENT = 0.28f

    /** Klasör arka planı — nötr colorSurface bazlı, yarı saydam. */
    const val FOLDER_SURFACE_ALPHA = 0.55f

    /** Çekmece arka planı — blur yerine düz yüzey, yüksek opasite (stok AllApps hissi). */
    const val DRAWER_SURFACE_ALPHA = 0.95f

    /** Çekmece arama çubuğu — tam yuvarlatılmış hap (pill) şekli. */
    val DRAWER_SEARCH_BAR_CORNER = 28.dp

    /** Uygulama/klasör etiket yazı boyutu — stok Android etiket hissi. */
    val LABEL_FONT_SIZE = 12.sp
    val FOLDER_LABEL_FONT_SIZE = 12.5.sp

    /** Etiket ağırlığı — sistem varsayılanı (Normal), özel ağırlık kullanılmaz. */
    // FontWeight.Normal composable tarafında uygulanır (import döngüsünü önlemek için burada tutulmaz).
}
