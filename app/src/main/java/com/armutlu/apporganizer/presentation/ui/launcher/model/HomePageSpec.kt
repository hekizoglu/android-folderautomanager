package com.armutlu.apporganizer.presentation.ui.launcher.model

import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder

/**
 * Ana ekran sayfasının deklaratif tanımı — Dashboard veya bir klasör grid sayfası.
 * [HomePagePlanner] tarafından saf biçimde üretilir; Compose/Android bağımlılığı yoktur.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm 3.1, Döngü P01.
 */
sealed interface HomePageSpec {
    val stableKey: String

    data object Dashboard : HomePageSpec {
        override val stableKey: String = "dashboard"
    }

    data class FolderPage(
        val pageIndex: Int,
        val firstFolderCategoryId: String?,
        val folders: List<AppFolder>,
    ) : HomePageSpec {
        override val stableKey: String =
            "folder:${firstFolderCategoryId ?: pageIndex}"
    }
}
