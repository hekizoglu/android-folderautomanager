package com.armutlu.apporganizer.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * C1: Rehber (Contacts) arama indeksleyici.
 *
 * Kullanıcı READ_CONTACTS iznini vermiş ve Ayarlar'da "Kişiler" kaynağını
 * açmışsa devreye girer. ContentObserver ile değişiklikleri izler; debounce
 * ile gereksiz reindex'i önler.
 */
@Singleton
class ContactsIndexer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchDao: SearchDao
) {

    companion object {
        private const val SOURCE_CONTACT = "contact"
        private const val GROUP_CONTACT = "contact"
        private const val DEBOUNCE_MS = 2_000L
        private const val MAX_CONTACTS = 500
    }

    private var observer: ContentObserver? = null
    private var debounceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Yüklü tüm kişileri search_documents tablosuna yazar. */
    suspend fun indexAll() {
        if (!hasPermission()) return
        if (!AppPrefs.isSearchSourceContactsEnabled(context)) return

        val docs = loadContacts()
        searchDao.deleteBySource(SOURCE_CONTACT)
        if (docs.isNotEmpty()) searchDao.insertAll(docs)
        Timber.d("ContactsIndexer: ${docs.size} kişi indekslendi")
    }

    /** Tüm kişi dökümanlarını search_documents'tan siler. */
    suspend fun clearIndex() {
        searchDao.deleteBySource(SOURCE_CONTACT)
        Timber.d("ContactsIndexer: kişi indeksi temizlendi")
    }

    /** ContentObserver kaydeder. Rehber değişince debounce ile reindex yapar. */
    fun registerObserver() {
        if (!hasPermission()) return
        if (observer != null) return

        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(DEBOUNCE_MS)
                    if (AppPrefs.isSearchSourceContactsEnabled(context)) {
                        indexAll()
                    }
                }
            }
        }.also {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, it
            )
        }
        Timber.d("ContactsIndexer: ContentObserver kaydedildi")
    }

    /** Observer'ı kaldırır. Uygulama kapanırken veya izin iptal edilince çağrılır. */
    fun unregisterObserver() {
        observer?.let {
            context.contentResolver.unregisterContentObserver(it)
            observer = null
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun loadContacts(): List<SearchDocument> {
        val docs = mutableListOf<SearchDocument>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.STARRED
        )
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL",
            null,
            "${ContactsContract.Contacts.STARRED} DESC, ${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        ) ?: return docs

        cursor.use { c ->
            val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = c.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            var count = 0
            while (c.moveToNext() && count < MAX_CONTACTS) {
                val id = c.getString(idIdx) ?: continue
                val name = c.getString(nameIdx) ?: continue
                if (name.isBlank()) continue
                val photoUri = c.getString(photoIdx) ?: ""
                docs.add(
                    SearchDocument(
                        sourceType = SOURCE_CONTACT,
                        sourceId = "contact:$id",
                        title = name,
                        subtitle = loadPrimaryPhone(id),
                        iconKey = if (photoUri.isNotEmpty()) "photo:$photoUri" else "contact:$id",
                        sourceGroup = GROUP_CONTACT,
                        lastModified = System.currentTimeMillis()
                    )
                )
                count++
            }
        }
        return docs
    }

    private fun loadPrimaryPhone(contactId: String): String {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ? AND " +
                "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} = 1",
            arrayOf(contactId),
            null
        ) ?: return ""
        return cursor.use { c ->
            if (c.moveToFirst()) c.getString(0) ?: "" else ""
        }
    }
}
