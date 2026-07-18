package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec

/**
 * Saf çözümleyici — [HomePageAnchor] (semantik "hangi sayfa") + mevcut [HomePageSpec] listesini
 * (gerçek sayfa planı) alıp gösterilecek Compose pager index'ine çevirir. Android/Compose
 * bağımlılığı yoktur, birim testlerinden doğrudan çağrılabilir.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P02 (satır 449-501).
 *
 * Fallback kuralları:
 * - `pages` boşsa her zaman 0 (çağıran taraf en az bir sayfa garanti etmeli — bkz. HomePagePlanner).
 * - Dashboard anchor'ı ama listede Dashboard yoksa (kapalıysa) -> ilk sayfa (ilk klasör sayfası).
 * - Folder(categoryId) anchor'ı ama o klasör silinmiş/bulunamıyorsa -> Dashboard varsa Dashboard
 *   sayfası, yoksa ilk sayfa.
 * - PageIndex(idx) sınır aşarsa (negatif veya listeden büyük) -> `coerceIn` ile clamp edilir.
 */
object HomePageAnchorResolver {

    fun resolve(pages: List<HomePageSpec>, anchor: HomePageAnchor): Int {
        if (pages.isEmpty()) return 0
        val lastIndex = pages.lastIndex

        return when (anchor) {
            is HomePageAnchor.Dashboard -> {
                pages.indexOfFirst { it is HomePageSpec.Dashboard }.takeIf { it >= 0 } ?: 0
            }

            is HomePageAnchor.Folder -> {
                val direct = pages.indexOfFirst {
                    it is HomePageSpec.FolderPage && it.firstFolderCategoryId == anchor.categoryId
                }
                if (direct >= 0) return direct
                // Klasör bulunamadı (silinmiş/taşınmış) -> güvenli fallback: Dashboard varsa
                // Dashboard, yoksa ilk sayfa (ilk klasör sayfası).
                pages.indexOfFirst { it is HomePageSpec.Dashboard }.takeIf { it >= 0 } ?: 0
            }

            is HomePageAnchor.PageIndex -> anchor.index.coerceIn(0, lastIndex)
        }
    }
}
