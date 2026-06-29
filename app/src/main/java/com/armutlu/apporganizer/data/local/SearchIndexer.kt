package com.armutlu.apporganizer.data.local

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.SearchDocument
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App ve Category varlıklarını SearchDocument'e dönüştüren indeksleyici.
 *
 * Sprint 1 kapsamı: Sadece App ve Category.
 * Sprint 2-3'te Contact ve File dönüşümleri eklenecek.
 *
 * Türkçe normalizasyon: title ve subtitle alanları lowercased + TR locale
 * ile normalize edilerek searchText'e yazılır. FTS5 tokenizer unicode61
 * ile birleşince İ→i, Ş→s, Ğ→g dönüşümü garantilenir.
 */
@Singleton
class SearchIndexer @Inject constructor() {

    companion object {
        private const val SOURCE_APP = "app"
        private const val SOURCE_CATEGORY = "category"
        private const val GROUP_APP = "app"
        private const val GROUP_CATEGORY = "category"
    }

    /**
     * AppInfo → SearchDocument dönüşümü.
     *
     * @param app Dönüştürülecek uygulama
     * @param categoryName Kategorinin görünen adı (subtitle olarak eklenir)
     */
    fun appToDocument(app: AppInfo, categoryName: String = ""): SearchDocument {
        val normalizedAppName = normalize(app.appName)
        val normalizedPkg = normalize(app.packageName)
        val normalizedCat = normalize(categoryName)
        val searchText = buildString {
            append(normalizedAppName)
            append(' ')
            append(normalizedPkg)
            if (normalizedCat.isNotBlank()) {
                append(' ')
                append(normalizedCat)
            }
        }
        return SearchDocument(
            sourceType = SOURCE_APP,
            sourceId = app.packageName,
            title = app.appName,
            subtitle = categoryName,
            iconKey = app.packageName,
            sourceGroup = GROUP_APP,
            lastModified = app.lastUpdated
        )
    }

    /**
     * Category → SearchDocument dönüşümü.
     */
    fun categoryToDocument(cat: Category): SearchDocument {
        val searchText = buildString {
            append(normalize(cat.categoryName))
            if (cat.description.isNotBlank()) {
                append(' ')
                append(normalize(cat.description))
            }
        }
        return SearchDocument(
            sourceType = SOURCE_CATEGORY,
            sourceId = cat.categoryId,
            title = cat.categoryName,
            subtitle = "",
            iconKey = "category:${cat.categoryId}",
            sourceGroup = GROUP_CATEGORY,
            lastModified = cat.createdAt
        )
    }

    /**
     * Toplu uygulama indeksleme. Kategori adlarını lookup için tüm kategori listesini alır.
     */
    fun indexApps(apps: List<AppInfo>, categories: List<Category>): List<SearchDocument> {
        val catNameMap = categories.associate { it.categoryId to it.categoryName }
        return apps.map { app ->
            appToDocument(app, categoryName = catNameMap[app.categoryId] ?: "")
        }
    }

    /**
     * Toplu kategori indeksleme.
     */
    fun indexCategories(categories: List<Category>): List<SearchDocument> =
        categories.map { categoryToDocument(it) }

    // ── helpers ────────────────────────────────────────────────────────────────

    /**
     * Türkçe locale ile lowercase normalizasyonu.
     * İ→i, I→ı dönüşümünü garantiler.
     */
    private fun normalize(text: String): String =
        text.lowercase(Locale("tr"))
}
