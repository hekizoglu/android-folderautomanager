package com.armutlu.apporganizer.domain.home

/**
 * Faz S4 — Sürükleme sırasında ekran kenarına yaklaşma tespiti (edge auto-scroll).
 *
 * SAF — Context, Room, Compose hiçbir bağımlılık YOK. Tamamen test edilebilir matematik.
 * Bu dosya sadece "parmak hangi kenara yakın?" sorusunu cevaplar; pager'ı gerçekten
 * kaydırmak (animateScrollToPage) çağıran Compose tarafının sorumluluğundadır.
 */

/** Kenar yakınlığı sonucu — pager'ın hangi yöne kaydırılması gerektiğini belirtir. */
enum class EdgeScrollDirection { NONE, PREVIOUS, NEXT }

/**
 * Verilen pointer X konumunun ([pointerXPx]) ekranın/composable'ın sol ya da sağ [edgeFraction]
 * kadarlık bandında olup olmadığını hesaplar.
 *
 * Sınır değerleri (tam [edgeFraction] veya [1 - edgeFraction] noktası) dahil edilir (inclusive) —
 * yani `pointerXPx <= screenWidthPx * edgeFraction` sol kenar, `pointerXPx >= screenWidthPx *
 * (1 - edgeFraction)` sağ kenar sayılır.
 */
fun detectEdgeScroll(
    pointerXPx: Float,
    screenWidthPx: Float,
    edgeFraction: Float = 0.12f,
): EdgeScrollDirection {
    if (screenWidthPx <= 0f) return EdgeScrollDirection.NONE

    val leftThreshold = screenWidthPx * edgeFraction
    val rightThreshold = screenWidthPx * (1f - edgeFraction)

    return when {
        pointerXPx <= leftThreshold -> EdgeScrollDirection.PREVIOUS
        pointerXPx >= rightThreshold -> EdgeScrollDirection.NEXT
        else -> EdgeScrollDirection.NONE
    }
}
