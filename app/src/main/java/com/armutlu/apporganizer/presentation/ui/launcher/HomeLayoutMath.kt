package com.armutlu.apporganizer.presentation.ui.launcher

import kotlin.math.max

/**
 * Klasör grid kapasite matematiği — FolderPager ölçüleriyle senkron tutulmalı:
 * satır = FolderTile (ikon folderSizeDp + etiket) , satır arası 16.dp,
 * grid dikey contentPadding 4.dp x2. Sayfa göstergesi + swipe hint için rezerv düşülür.
 *
 * Amaç: klasörler ASLA kırpılmasın — sığmayan klasör sonraki sayfaya taşınır.
 */
object HomeLayoutMath {
    const val ROW_SPACING_DP = 16
    const val GRID_VERTICAL_PADDING_DP = 8
    /** Klasör etiketi + sayı satırı yaklaşık yüksekliği (FolderTile içi metinler). */
    const val LABEL_HEIGHT_DP = 36
    /** HomePageIndicator + SwipeHint için ayrılan pay. */
    const val INDICATOR_RESERVE_DP = 36

    /**
     * Verilen yükseklikte kırpılmadan sığan klasör sayısı. En az 1 satır garanti edilir
     * (aşırı küçük pencere durumunda bile grid boş kalmaz).
     */
    fun folderCapacity(availableHeightDp: Int, folderSizeDp: Int, columns: Int): Int {
        val rowHeight = folderSizeDp + LABEL_HEIGHT_DP
        val usable = availableHeightDp - GRID_VERTICAL_PADDING_DP - INDICATOR_RESERVE_DP
        if (usable <= rowHeight) return columns
        // n satır koşulu: n*rowHeight + (n-1)*spacing <= usable
        val rows = max(1, (usable + ROW_SPACING_DP) / (rowHeight + ROW_SPACING_DP))
        return rows * columns
    }
}
