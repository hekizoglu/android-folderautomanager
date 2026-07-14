package com.armutlu.apporganizer.data.repository

import android.content.Context
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.ContactsIndexer
import com.armutlu.apporganizer.data.local.FilesIndexer
import com.armutlu.apporganizer.data.local.FilesIndexWorker
import com.armutlu.apporganizer.data.local.SearchDao
import com.armutlu.apporganizer.data.local.SearchIndexer
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import com.armutlu.apporganizer.utils.SystemSettingsCatalog
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class SearchRepository(
    private val context: Context,
    private val searchDao: SearchDao,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val indexer: SearchIndexer,
    private val contactsIndexer: ContactsIndexer,
    private val filesIndexer: FilesIndexer,
    private val db: AppDatabase
) {

    internal companion object {
        fun buildLikePattern(rawQuery: String): String =
            "%${rawQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"
    }

    // FTS5 runtime check — bazı AOSP build'lerinde fts5 modülü yoktur
    private var settingsSeeded = false

    private val fts5Available: Boolean by lazy {
        try {
            AppDatabase.isFts5Available(db.openHelper.writableDatabase)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun search(rawQuery: String, limit: Int = 50): Map<SourceType, List<SearchDocument>> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyMap()

        var result: Map<SourceType, List<SearchDocument>> = emptyMap()
        val elapsedMs = measureTimeMillis {
            result = withContext(Dispatchers.IO) {
                val allowedSources = enabledSources()
                if (allowedSources.isEmpty()) return@withContext emptyMap()
                ensureSettingsIndexedIfNeeded(allowedSources)
                runCatching {
                    val docs = if (fts5Available) {
                        val ftsQuery = trimmed.split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                            .joinToString(" ") { "\"${it.replace("\"", "\"\"")}\"*" }
                        searchDao.search(buildFts5Query(ftsQuery, limit, allowedSources))
                    } else {
                        searchDao.search(buildLikeQuery(trimmed, limit, allowedSources))
                    }
                    docs.groupBy { SourceType.fromKey(it.sourceType) }
                }.getOrElse { e ->
                    Timber.w(e, "search hatası, LIKE fallback deneniyor")
                    runCatching {
                        val docs = searchDao.search(buildLikeQuery(trimmed, limit, allowedSources))
                        docs.groupBy { SourceType.fromKey(it.sourceType) }
                    }.getOrDefault(emptyMap())
                }
            }
        }

        if (AppPrefs.isSearchStatsEnabled(context)) {
            val resultCount = result.values.sumOf { it.size }
            SearchStatsPrefs.logSearch(context, trimmed.length, resultCount, elapsedMs)
        }

        return result
    }

    private fun enabledSources(): List<String> = buildList {
        add(SourceType.APP.key)
        if (AppPrefs.isSearchSourceCategoriesEnabled(context)) add(SourceType.CATEGORY.key)
        if (AppPrefs.isSearchSourceSettingsEnabled(context)) add(SourceType.SETTING.key)
        if (AppPrefs.isSearchSourceContactsEnabled(context)) add(SourceType.CONTACT.key)
        if (AppPrefs.isSearchSourceFilesEnabled(context)) add(SourceType.FILE.key)
    }

    private suspend fun ensureSettingsIndexedIfNeeded(allowedSources: List<String>) {
        if (settingsSeeded || SourceType.SETTING.key !in allowedSources) return
        searchDao.deleteBySource(SourceType.SETTING.key)
        searchDao.insertAll(SystemSettingsCatalog.documents())
        settingsSeeded = true
    }

    private fun buildFts5Query(ftsQuery: String, limit: Int, allowedSources: List<String>): SimpleSQLiteQuery {
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
                        WHEN 'setting' THEN 2
                        WHEN 'contact' THEN 3
                        WHEN 'file' THEN 4
                        ELSE 9
                    END ASC,
                    bm25(search_fts) ASC,
                    title ASC
                LIMIT ?
            """.trimIndent(),
            args,
        )
    }

    private fun buildLikeQuery(rawQuery: String, limit: Int, allowedSources: List<String>): SimpleSQLiteQuery {
        val pattern = buildLikePattern(rawQuery)
        val placeholders = allowedSources.joinToString(",") { "?" }
        val args = (listOf<Any>(pattern, pattern) + allowedSources + limit).toTypedArray()
        return SimpleSQLiteQuery(
            """
                SELECT * FROM search_documents
                WHERE (title LIKE ? ESCAPE '\' OR subtitle LIKE ? ESCAPE '\')
                    AND source_type IN ($placeholders)
                ORDER BY
                    CASE source_group
                        WHEN 'app' THEN 0
                        WHEN 'category' THEN 1
                        WHEN 'setting' THEN 2
                        WHEN 'contact' THEN 3
                        WHEN 'file' THEN 4
                        ELSE 9
                    END ASC,
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
            docs.addAll(SystemSettingsCatalog.documents())

            searchDao.deleteAll()
            searchDao.insertAll(docs)

            Timber.d("bootstrapIndex: ${docs.size} döküman indekslendi (${apps.size} app, ${categories.size} kategori)")

            // Rehber ve dosya kaynakları açıksa paralel indeksle
            if (AppPrefs.isSearchSourceContactsEnabled(context)) {
                contactsIndexer.indexAll()
            }
            if (AppPrefs.isSearchSourceFilesEnabled(context)) {
                filesIndexer.indexAll()
            }
        }.onFailure {
            Timber.e(it, "bootstrapIndex hatası")
        }
    }

    /** Rehber kaynağı açılınca veya izin verilince çağrılır. */
    suspend fun enableContactsSource() = withContext(Dispatchers.IO) {
        contactsIndexer.indexAll()
        contactsIndexer.registerObserver()
    }

    /** Rehber kaynağı kapatılınca. */
    suspend fun disableContactsSource() = withContext(Dispatchers.IO) {
        contactsIndexer.unregisterObserver()
        contactsIndexer.clearIndex()
    }

    /** Dosya kaynağı açılınca çağrılır. */
    suspend fun enableFilesSource() = withContext(Dispatchers.IO) {
        FilesIndexWorker.enqueueNow(context)
        FilesIndexWorker.schedule(context)
    }

    /** Dosya kaynağı kapatılınca. */
    suspend fun disableFilesSource() = withContext(Dispatchers.IO) {
        FilesIndexWorker.cancel(context)
        filesIndexer.clearIndex()
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
