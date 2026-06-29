package com.armutlu.apporganizer.data.repository

import android.content.Context
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.SearchDao
import com.armutlu.apporganizer.data.local.SearchIndexer
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

/**
 * Birleşik arama repository'si.
 *
 * Sorumluluklar:
 * - FTS5 prefix-match sorgusunu çalıştırmak (Sprint 1: App + Category)
 * - İlk indekslemeyi (bootstrap) yapmak
 * - Delta güncellemeleri (app ekleme/silme, kategori değişimi) indekse yansıtmak
 *
 * Sprint 2-3: Contacts ve Files indeksleme buraya eklenecek.
 */
@Singleton
class SearchRepository(
    private val context: Context,
    private val searchDao: SearchDao,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val indexer: SearchIndexer
) {

    /**
     * Prefix-match arama.
     *
     * Sorgu "wa" → FTS5'te `"wa"*` olarak çalışır, "WhatsApp" ve "Walmart" ile eşleşir.
     * Sonuçlar sourceGroup (app→category) sırasında, grup içi bm25 ile sıralanır.
     *
     * @param rawQuery Kullanıcının yazdığı ham metin
     * @param limit Maksimum sonuç sayısı
     * @return sourceType'a göre gruplandırılmış sonuçlar
     */
    suspend fun search(rawQuery: String, limit: Int = 50): Map<SourceType, List<SearchDocument>> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyMap()

        // FTS5 prefix-match: her terimin sonuna * ekle
        val ftsQuery = trimmed.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { "\"${it.replace("\"", "\"\"")}\"*" }

        return withContext(Dispatchers.IO) {
            val allowedSources = enabledSources()
            if (allowedSources.isEmpty()) return@withContext emptyMap()
            val docs = searchDao.search(buildSearchQuery(ftsQuery, limit, allowedSources))
            docs.groupBy { SourceType.fromKey(it.sourceType) }
        }
    }

    private fun enabledSources(): List<String> = buildList {
        if (AppPrefs.isSearchSourceAppsEnabled(context)) add(SourceType.APP.key)
        if (AppPrefs.isSearchSourceCategoriesEnabled(context)) add(SourceType.CATEGORY.key)
        if (AppPrefs.isSearchSourceContactsEnabled(context)) add(SourceType.CONTACT.key)
        if (AppPrefs.isSearchSourceFilesEnabled(context)) add(SourceType.FILE.key)
    }

    private fun buildSearchQuery(ftsQuery: String, limit: Int, allowedSources: List<String>): SimpleSQLiteQuery {
        val placeholders = allowedSources.joinToString(",") { "?" }
        val args = (listOf<Any>(ftsQuery) + allowedSources + limit).toTypedArray()
        return SimpleSQLiteQuery(
            """
                SELECT search_documents.*
                FROM search_documents
                JOIN search_fts ON search_documents.docId = search_fts.rowid
                WHERE search_fts MATCH ?
                    AND source_type IN ($placeholders)
                ORDER BY
                    CASE source_group
                        WHEN 'app' THEN 0
                        WHEN 'category' THEN 1
                        WHEN 'contact' THEN 2
                        WHEN 'file' THEN 3
                        ELSE 9
                    END ASC,
                    bm25(search_fts) ASC,
                    title ASC
                LIMIT ?
            """.trimIndent(),
            args,
        )
    }

    /**
     * İlk indekslemeyi (bootstrap) yapar.
     * Mevcut tüm app ve kategorileri SearchDocument'e dönüştürüp FTS tablosuna yazar.
     *
     * Çağrı yeri: Uygulama ilk açıldığında, DB migration sonrası, veya tam reindex gerektiğinde.
     */
    suspend fun bootstrapIndex() = withContext(Dispatchers.IO) {
        runCatching {
            val apps = appDao.getAllApps()
            val categories = categoryDao.getAllCategories()
            val docs = mutableListOf<SearchDocument>()

            docs.addAll(indexer.indexApps(apps, categories))
            docs.addAll(indexer.indexCategories(categories))

            searchDao.deleteAll()
            searchDao.insertAll(docs)

            Timber.d("bootstrapIndex: ${docs.size} döküman indekslendi (${apps.size} app, ${categories.size} kategori)")
        }.onFailure {
            Timber.e(it, "bootstrapIndex hatası")
        }
    }

    /**
     * Yeni yüklenen uygulamayı indekse ekler.
     * LauncherViewModel.onPackageAdded()'den çağrılır.
     */
    suspend fun indexApp(app: com.armutlu.apporganizer.domain.models.AppInfo) = withContext(Dispatchers.IO) {
        runCatching {
            val categories = categoryDao.getAllCategories()
            val catName = categories.find { it.categoryId == app.categoryId }?.categoryName ?: ""
            val doc = indexer.appToDocument(app, catName)
            searchDao.delete("app", app.packageName)
            searchDao.insert(doc)
            Timber.d("indexApp: ${app.packageName} indekslendi")
        }.onFailure {
            Timber.e(it, "indexApp hatası: ${app.packageName}")
        }
    }

    /**
     * Kaldırılan uygulamayı indeksten siler.
     * LauncherViewModel.onPackageRemoved()'den çağrılır.
     */
    suspend fun removeApp(packageName: String) = withContext(Dispatchers.IO) {
        runCatching {
            searchDao.delete("app", packageName)
            Timber.d("removeApp: $packageName indeksten silindi")
        }.onFailure {
            Timber.e(it, "removeApp hatası: $packageName")
        }
    }

    /**
     * Kategori değiştiğinde indeksi günceller.
     * - Kategori adı değişmişse: o kategorideki app'lerin subtitle'ını güncelle + kategori dökümanını güncelle
     * - Kategori silinmişse: kategori dökümanını sil
     */
    suspend fun reindexCategory(oldCategory: com.armutlu.apporganizer.domain.models.Category?,
                                newCategory: com.armutlu.apporganizer.domain.models.Category) = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            if (oldCategory != null && oldCategory.categoryName != newCategory.categoryName) {
                searchDao.updateCategoryRefs(oldCategory.categoryName, newCategory.categoryName, now)
            }
            val doc = indexer.categoryToDocument(newCategory).copy(lastModified = now)
            searchDao.delete("category", newCategory.categoryId)
            searchDao.insert(doc)
            Timber.d("reindexCategory: ${newCategory.categoryId}")
        }.onFailure {
            Timber.e(it, "reindexCategory hatası: ${newCategory.categoryId}")
        }
    }

    /**
     * Silinen kategoriyi indeksten kaldırır.
     */
    suspend fun removeCategory(categoryId: String) = withContext(Dispatchers.IO) {
        runCatching {
            searchDao.delete("category", categoryId)
            Timber.d("removeCategory: $categoryId indeksten silindi")
        }.onFailure {
            Timber.e(it, "removeCategory hatası: $categoryId")
        }
    }

    /**
     * Debug/test için indekslenmiş döküman sayısı.
     */
    suspend fun count(): Int = withContext(Dispatchers.IO) {
        runCatching { searchDao.count() }.getOrDefault(0)
    }
}
