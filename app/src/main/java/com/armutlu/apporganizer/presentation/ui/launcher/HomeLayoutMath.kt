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
    const val MIN_VISIBLE_FOLDERS = 4
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
        if (usable <= rowHeight) return max(MIN_VISIBLE_FOLDERS, columns)
        // n satır koşulu: n*rowHeight + (n-1)*spacing <= usable
        val rows = max(1, (usable + ROW_SPACING_DP) / (rowHeight + ROW_SPACING_DP))
        return max(MIN_VISIBLE_FOLDERS, rows * columns)
    }

    fun pageSize(requestedPageSize: Int, folderCapacity: Int): Int =
        minOf(requestedPageSize, max(MIN_VISIBLE_FOLDERS, folderCapacity))

    /**
     * Klasör sayfa sayısı — Döngü P04'ten itibaren HomeScreen.kt bu fonksiyonu doğrudan çağırır
     * (eskiden inline `maxOf(1, (displayFolders.size + pageSize - 1) / pageSize)` kopyası vardı).
     * En az 1 sayfa garanti edilir (klasör olmasa bile Dashboard/boş sayfa gösterilir).
     */
    fun pageCount(folderCount: Int, pageSize: Int): Int {
        val safePageSize = max(1, pageSize)
        return max(1, (folderCount + safePageSize - 1) / safePageSize)
    }

    /**
     * Ekran genişliğine göre klasör grid sütun sayısı — HomeScreen.kt satır ~170-174 ve
     * HomeScreenFolderPager.kt satır ~73-78 ile senkron tutulmalı (breakpoint: 600dp/840dp).
     */
    fun screenColumns(screenWidthDp: Int): Int = when {
        screenWidthDp >= 840 -> 6
        screenWidthDp >= 600 -> 5
        else -> 4
    }
}
