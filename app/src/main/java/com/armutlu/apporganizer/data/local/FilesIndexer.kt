package com.armutlu.apporganizer.data.local

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.utils.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

/**
 * C2: Dosya arama indeksleyici (MediaStore tabanlı).
 *
 * Kullanıcı Ayarlar'da "Dosya Adları" kaynağını açtığında devreye girer.
 * Ek runtime izni gerektirmez — MediaStore'a launcher her zaman erişebilir.
 * Büyük koleksiyonlarda performansı korumak için LIMIT uygulanır.
 *
 * WorkManager ile periyodik reindex: FilesIndexWorker ayrı class'ta.
 */
@Singleton
class FilesIndexer(
    private val context: Context,
    private val searchDao: SearchDao
) {

    companion object {
        private const val SOURCE_FILE = "file"
        private const val GROUP_FILE = "file"
        private const val MAX_FILES = 1000
    }

    /** MediaStore'dan dosya adlarını indeksler. */
    suspend fun indexAll() = withContext(Dispatchers.IO) {
        if (!AppPrefs.isSearchSourceFilesEnabled(context)) return@withContext
        if (!hasMediaStoreReadAccess()) {
            Timber.w("FilesIndexer: dosya arama izni yok, indeksleme atlandi")
            return@withContext
        }

        val docs = loadFiles()
        searchDao.deleteBySource(SOURCE_FILE)
        if (docs.isNotEmpty()) searchDao.insertAll(docs)
        Timber.d("FilesIndexer: ${docs.size} dosya indekslendi")
    }

    /** Tüm dosya dökümanlarını temizler. */
    suspend fun clearIndex() = withContext(Dispatchers.IO) {
        searchDao.deleteBySource(SOURCE_FILE)
        Timber.d("FilesIndexer: dosya indeksi temizlendi")
    }

    private fun loadFiles(): List<SearchDocument> {
        val docs = mutableListOf<SearchDocument>()
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "image/*",
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "video/*",
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to "audio/*",
            getDownloadsUri() to "application/*"
        )

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )

        var total = 0
        for ((uri, mimeHint) in collections) {
            if (total >= MAX_FILES) break
            val cursor = try {
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )
            } catch (e: Exception) {
                Timber.w(e, "FilesIndexer: MediaStore query skipped: $uri")
                continue
            } ?: continue

            cursor.use { c ->
                val idIdx = c.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathIdx = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val dateIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                while (c.moveToNext() && total < MAX_FILES) {
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
                    total++
                }
            }
        }
        return docs
    }

    private fun getDownloadsUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

    private fun hasMediaStoreReadAccess(): Boolean {
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
