package com.armutlu.apporganizer.presentation.ui.launcher.model

import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder

/**
 * Ana ekran sayfasının deklaratif tanımı — Dashboard, Widget sayfası veya bir klasör grid sayfası.
 * [HomePagePlanner] tarafından saf biçimde üretilir; Compose/Android bağımlılığı yoktur.
 *
 * Döngü P0.6 — Widget sistem refaktörü: Seçim A uygulanıyor (ayrı sayfa).
 * Widget sayfası (WidgetPageSpec) Dashboard'un yanında, klasörlerin ayrı sayfasından önce yerleşir;
 * sayfa sırası: Dashboard (0) → WidgetPage (1) → Klasör sayfaları (2+).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm 3.1, Döngü P01.
 */
sealed interface HomePageSpec {
    val stableKey: String

    data object Dashboard : HomePageSpec {
        override val stableKey: String = "dashboard"
    }

    data object WidgetPage : HomePageSpec {
        override val stableKey: String = "widget_page"
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
