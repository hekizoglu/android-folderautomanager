package com.armutlu.apporganizer.data.local

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.armutlu.apporganizer.domain.models.FileIndexState
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.computeFileIndexState
import com.armutlu.apporganizer.utils.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

/**
 * C2: Dosya arama indeksleyici (MediaStore tabanlı).
 *
 * Kullanıcı Ayarlar'da "Dosya Adları" kaynağını açtığında devreye girer.
 * Ek runtime izni gerektirmez - MediaStore'a launcher her zaman erişebilir.
 * Büyük koleksiyonlarda performansı korumak için LIMIT uygulanır.
 *
 * WorkManager ile periyodik reindex: FilesIndexWorker ayrı class'ta.
 *
 * P0.3: İzin yokken indeksleme artık sessizce atlanmıyor - [indexState] StateFlow'u
 * Disabled/PermissionRequired/Indexing/Ready/Failed durumlarından birini yayınlar,
 * böylece SearchSettingsScreen ve arama sonuç UI'ları "izin yok" ile "0 sonuç"u ayırt edebilir.
 */
@Singleton
class FilesIndexer(
    private val context: Context,
    private val searchDao: SearchDao
) {

    companion object {
        private const val SOURCE_FILE = "file"
        private const val GROUP_FILE = "file"

        // Dosya türü kotaları (toplam ~9000 dosya)
        private const val QUOTA_IMAGES = 3000
        private const val QUOTA_VIDEOS = 1000
        private const val QUOTA_AUDIO = 1000
        private const val QUOTA_DOWNLOADS = 1000
        private const val PAGINATION_LIMIT = 500  // Cursor sayfası boyutu

        fun hasMediaStoreReadAccess(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                )
            } else {
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            return permissions.any { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private val _indexState = MutableStateFlow(currentState(isIndexing = false))
    val indexState: StateFlow<FileIndexState> = _indexState.asStateFlow()

    // WorkManager progress callback (opsiyonel)
    var progressCallback: ((processedCount: Int, totalCount: Int) -> Unit)? = null

    private fun currentState(isIndexing: Boolean): FileIndexState = computeFileIndexState(
        sourceEnabled = AppPrefs.isSearchSourceFilesEnabled(context),
        hasPermission = hasMediaStoreReadAccess(context),
        isIndexing = isIndexing,
        lastFailureReason = AppPrefs.getFileIndexFailureReason(context),
        itemCount = AppPrefs.getFileIndexItemCount(context),
        lastIndexedAt = AppPrefs.getFileIndexLastIndexedAt(context),
    )

    /** Ayarlar ekranı veya arama UI'ı açılırken/geri dönerken güncel durumu yeniden hesaplar. */
    fun refreshState() {
        _indexState.value = currentState(isIndexing = false)
    }

    /** MediaStore'dan dosya adlarını indeksler. */
    suspend fun indexAll() = withContext(Dispatchers.IO) {
        if (!AppPrefs.isSearchSourceFilesEnabled(context)) {
            _indexState.value = currentState(isIndexing = false)
            return@withContext
        }
        if (!hasMediaStoreReadAccess(context)) {
            Timber.w("FilesIndexer: dosya arama izni yok, indeksleme atlandi")
            _indexState.value = currentState(isIndexing = false)
            return@withContext
        }

        clearStalePersistedUriPermissions()
        _indexState.value = FileIndexState.Indexing()
        try {
            val docs = loadFiles()
            searchDao.deleteBySource(SOURCE_FILE)
            if (docs.isNotEmpty()) searchDao.insertAll(docs)
            val now = System.currentTimeMillis()
            AppPrefs.setFileIndexSuccess(context, docs.size, now)
            _indexState.value = FileIndexState.Ready(docs.size, now)
            Timber.d("FilesIndexer: ${docs.size} dosya indekslendi")
        } catch (e: Exception) {
            val reason = e.message ?: e.javaClass.simpleName
            AppPrefs.setFileIndexFailure(context, reason)
            Timber.e(e, "FilesIndexer: indeksleme hatasi")
            _indexState.value = currentState(isIndexing = false)
        }
    }

    /** Tüm dosya dökümanlarını temizler. */
    suspend fun clearIndex() = withContext(Dispatchers.IO) {
        searchDao.deleteBySource(SOURCE_FILE)
        AppPrefs.clearFileIndexState(context)
        _indexState.value = currentState(isIndexing = false)
        Timber.d("FilesIndexer: dosya indeksi temizlendi")
    }

    /**
     * İndeks başlangıcında geçersiz kalmış persisted URI izinlerini temizler
     * (kullanıcı SAF ile klasör seçtiyse ve o klasör silindi/taşındıysa).
     * MediaStore erişimi bu izinlere bağımlı değildir ama artık kullanılmayan
     * izinler ContentResolver.getPersistedUriPermissions() listesinde birikip
     * "izin var" izlenimi yaratabilir - spec madde 5.
     */
    private fun clearStalePersistedUriPermissions() {
        runCatching {
            val resolver = context.contentResolver
            resolver.persistedUriPermissions.forEach { perm ->
                val stillAccessible = runCatching {
                    resolver.query(perm.uri, null, null, null, null)?.use { true } ?: false
                }.getOrDefault(false)
                if (!stillAccessible) {
                    runCatching {
                        resolver.releasePersistableUriPermission(
                            perm.uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    Timber.d("FilesIndexer: gecersiz persisted URI izni temizlendi: ${perm.uri}")
                }
            }
        }.onFailure { Timber.w(it, "FilesIndexer: persisted URI temizligi basarisiz") }
    }

    private fun loadFiles(): List<SearchDocument> {
        val docs = mutableListOf<SearchDocument>()
        val totalQuota = QUOTA_IMAGES + QUOTA_VIDEOS + QUOTA_AUDIO + QUOTA_DOWNLOADS

        // Dosya türü başına kota ve URI
        val collections = listOf(
            Triple(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*", QUOTA_IMAGES),
            Triple(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*", QUOTA_VIDEOS),
            Triple(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*", QUOTA_AUDIO),
            Triple(getDownloadsUri(), "application/*", QUOTA_DOWNLOADS)
        )

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )

        for ((uri, mimeHint, quota) in collections) {
            var typeCount = 0
            var offset = 0

            // Pagination döngüsü: offset += PAGINATION_LIMIT
            while (typeCount < quota) {
                val cursor = try {
                    val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $PAGINATION_LIMIT OFFSET $offset"
                    context.contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        sortOrder
                    )
                } catch (e: Exception) {
                    Timber.w(e, "FilesIndexer: MediaStore query skipped for $uri at offset $offset")
                    break
                } ?: break

                cursor.use { c ->
                    if (c.count == 0) break  // Sayfada veri yok - son sayfa

                    val idIdx = c.getColumnIndex(MediaStore.MediaColumns._ID)
                    val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val pathIdx = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    val dateIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                    while (c.moveToNext() && typeCount < quota) {
                        val id = c.getLong(if (idIdx >= 0) idIdx else continue)
                        val name = c.getString(if (nameIdx >= 0) nameIdx else continue) ?: continue
                        if (name.isBlank()) continue

                        val path = if (pathIdx >= 0) c.getString(pathIdx) ?: "" else ""
                        val dateModified = if (dateIdx >= 0) c.getLong(dateIdx) * 1000L else 0L
                        val mime = if (mimeIdx >= 0) c.getString(mimeIdx) ?: mimeHint else mimeHint
                        val fileUri = ContentUris.withAppendedId(uri, id).toString()

                        docs.add(
                            SearchDocument(
                                sourceType = SOURCE_FILE,
                                sourceId = fileUri,
                                title = name,
                                subtitle = path,
                                iconKey = "mime:$mime",
                                sourceGroup = GROUP_FILE,
                                lastModified = dateModified
                            )
                        )
                        typeCount++

                        // Progress callback
                        progressCallback?.invoke(docs.size, totalQuota)
                    }
                }
                offset += PAGINATION_LIMIT
            }

            Timber.d("FilesIndexer: $typeCount dosya indekslendi (kaynak: $uri, quota: $quota)")
        }

        return docs
    }

    private fun getDownloadsUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

}
