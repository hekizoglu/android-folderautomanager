package com.armutlu.apporganizer.domain.usecase.folder

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

/**
 * R4.2 madde 5: Boş sistem klasörünü görünür listeden düşür fakat veritabanından silme.
 * UI rendering sırasında boş klasörleri filtrele; persistence'da tutma.
 */
object EmptyFolderFilter {

    /**
     * Verilen kategori listesinden boş olanları (hiç uygulama yok) filtrele.
     * Boş kategoriler DB'de kalır ama UI'da gösterilmez.
     *
     * @param categories Tüm kategoriler (boş dahil)
     * @param allApps Tüm uygulamalar (kategoriye göre gruplanacak)
     * @return UI'da gösterilecek kategoriler (boş değil)
     */
    fun filterNonEmpty(
        categories: List<Category>,
        allApps: List<AppInfo>,
    ): List<Category> {
        val appsPerCategory = allApps.groupBy { it.categoryId }

        return categories.filter { category ->
            val appCount = appsPerCategory[category.categoryId]?.size ?: 0
            appCount > 0 // Boş kategoriler (0 app) filtrele
        }
    }

    /**
     * Belirli bir klasörün boş olup olmadığını kontrol et.
     *
     * @param categoryId Klasör kategorisi
     * @param allApps Tüm uygulamalar
     * @return true: boş, false: uygulama var
     */
    fun isCategoryEmpty(categoryId: String, allApps: List<AppInfo>): Boolean {
        return !allApps.any { it.categoryId == categoryId }
    }
}
