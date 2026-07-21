package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import kotlin.math.max

/**
 * Ana ekran sayfa planını üreten saf hesaplayıcı — Compose/Android bağımlılığı yoktur,
 * doğrudan birim testlerinden çağrılabilir.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm 3.2, Döngü P01.
 *
 * [HomeLayoutMath.pageCount] ile tutarlı kalmalıdır: klasör sayfası adedi her zaman
 * `pageCount(folders.size, pageSize)` ile eşleşir (Dashboard sayfası ayrıca eklenir).
 */
object HomePagePlanner {

    /** pageSize <= 0 gelirse kullanılacak güvenli minimum (HomeLayoutMath.MIN_VISIBLE_FOLDERS ile tutarlı). */
    private const val SAFE_MIN_PAGE_SIZE = HomeLayoutMath.MIN_VISIBLE_FOLDERS

    /** Üretim Hero yolu: Sayfa 0 koşulsuz Dashboard'dur; çağıran feature flag veremez. */
    fun buildHeroPages(
        folders: List<AppFolder>,
        pageSize: Int,
    ): List<HomePageSpec> = buildPages(
        folders = folders,
        pageSize = pageSize,
        dashboardEnabled = true,
    )

    /**
     * @param folders Kategoriye göre gruplanmış, boş olmayan klasörler (buildFolders() çıktısı).
     * @param pageSize Bir sayfada gösterilecek maksimum klasör sayısı (ör. HomeLayoutMath.pageSize sonucu).
     * @param dashboardEnabled Dashboard sayfası gösterilsin mi (P24'te kullanıcı ayarına bağlanacak).
     *
     * Kurallar:
     * - Dashboard açıksa ilk eleman daima [HomePageSpec.Dashboard].
     * - Klasör listesi `chunked(pageSize)` ile bölünür, her dilim bir [HomePageSpec.FolderPage] olur.
     * - Klasör yoksa Dashboard açıkken sadece Dashboard gösterilir; kapalıyken boş bir
     *   FolderPage(pageIndex=0, folders=emptyList()) fallback'i döner — en az bir sayfa garantisi.
     * - Dashboard kapalı klasik modda ilk klasör sayfası pageIndex=0 olur.
     * - Sonuç listesinde stableKey her zaman benzersizdir (duplicate categoryId durumunda
     *   pageIndex'e düşülür).
     */
    internal fun buildPages(
        folders: List<AppFolder>,
        pageSize: Int,
        dashboardEnabled: Boolean,
    ): List<HomePageSpec> {
        val safePageSize = max(SAFE_MIN_PAGE_SIZE, pageSize)

        val folderPages: List<HomePageSpec.FolderPage> = if (folders.isEmpty()) {
            listOf(HomePageSpec.FolderPage(pageIndex = 0, firstFolderCategoryId = null, folders = emptyList()))
        } else {
            folders.chunked(safePageSize).mapIndexed { index, chunk ->
                HomePageSpec.FolderPage(
                    pageIndex = index,
                    firstFolderCategoryId = chunk.first().category.categoryId,
                    folders = chunk,
                )
            }
        }

        val planned: List<HomePageSpec> = if (dashboardEnabled) {
            if (folders.isEmpty()) {
                listOf(HomePageSpec.Dashboard)
            } else {
                listOf(HomePageSpec.Dashboard) + folderPages
            }
        } else {
            folderPages
        }

        return dedupeStableKeys(planned).ifEmpty {
            listOf(HomePageSpec.FolderPage(pageIndex = 0, firstFolderCategoryId = null, folders = emptyList()))
        }
    }

    /**
     * Aynı stableKey'e sahip birden fazla sayfa oluşursa (ör. iki farklı sayfa aynı
     * firstFolderCategoryId ile başlarsa — normal akışta olmaz ama savunma amaçlı),
     * ikinci ve sonraki çakışan FolderPage'leri pageIndex tabanlı anahtara düşürür.
     */
    private fun dedupeStableKeys(pages: List<HomePageSpec>): List<HomePageSpec> {
        val seen = mutableSetOf<String>()
        return pages.map { page ->
            if (seen.add(page.stableKey)) {
                page
            } else if (page is HomePageSpec.FolderPage) {
                val fallbackKey = "folder:${page.pageIndex}"
                seen.add(fallbackKey)
                page.copy(firstFolderCategoryId = null).let {
                    // firstFolderCategoryId=null zorunlu olarak stableKey'i "folder:pageIndex" yapar.
                    it
                }
            } else {
                page
            }
        }
    }
}
