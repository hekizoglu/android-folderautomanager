package com.armutlu.apporganizer.utils

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tek kaynak, çift index: AppCache + ContactCache
 *
 * App isimleri:
 *   • normalized Map<String,AppInfo>  — lowercase TR fold
 *   • prefixIndex Map<String, List<AppInfo>> — her prefix (1-4 char) → apps
 *
 * Kişiler:
 *   • ContactEntry listesi (id, displayName, phone, photoUri)
 *   • prefixIndex Map<String, List<ContactEntry>>
 *
 * Fuzzy: trigram setleri üretilir, query trigramları ile kesişim skoru hesaplanır.
 * Fonetik (ASCII-fold): ş→s, ü→u, ö→o, ç→c, ğ→g, ı→i
 */
object SearchCache {

    data class ContactEntry(
        val id: Long,
        val displayName: String,
        val phone: String,
        val photoUri: String?,
        // precomputed
        val normalized: String = asciiFold(displayName.lowercase()),
        val trigrams: Set<String> = trigramSet(asciiFold(displayName.lowercase()))
    )

    // ── App cache ─────────────────────────────────────────────────────────────

    @Volatile private var appList: List<AppInfo> = emptyList()
    @Volatile private var appNormMap: Map<String, AppInfo> = emptyMap()        // normalized name → app
    @Volatile private var appPrefixIndex: Map<String, List<AppInfo>> = emptyMap()
    @Volatile private var appTrigramIndex: Map<String, List<AppInfo>> = emptyMap()

    fun warmApps(apps: List<AppInfo>) {
        appList = apps
        val normMap = HashMap<String, AppInfo>(apps.size * 2)
        val prefixIdx = HashMap<String, MutableList<AppInfo>>()
        val trigramIdx = HashMap<String, MutableList<AppInfo>>()

        for (app in apps) {
            val norm = asciiFold(app.appName.lowercase())
            normMap[norm] = app

            // Prefix index: her 1-4 uzunluklu prefix
            for (len in 1..minOf(4, norm.length)) {
                val prefix = norm.substring(0, len)
                prefixIdx.getOrPut(prefix) { mutableListOf() }.add(app)
            }

            // Trigram index
            for (tri in trigramSet(norm)) {
                trigramIdx.getOrPut(tri) { mutableListOf() }.add(app)
            }
        }
        appNormMap = normMap
        appPrefixIndex = prefixIdx
        appTrigramIndex = trigramIdx
    }

    // ── Contact cache ─────────────────────────────────────────────────────────

    @Volatile private var contactList: List<ContactEntry> = emptyList()
    @Volatile private var contactPrefixIndex: Map<String, List<ContactEntry>> = emptyMap()
    @Volatile private var contactTrigramIndex: Map<String, List<ContactEntry>> = emptyMap()

    private var contactObserver: ContentObserver? = null
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun loadContacts(context: Context, onLoaded: (() -> Unit)? = null) {
        cacheScope.launch {
            val entries = queryContacts(context)
            indexContacts(entries)
            onLoaded?.invoke()
        }
    }

    fun observeContacts(context: Context) {
        if (contactObserver != null) return
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                loadContacts(context)
            }
        }
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI, true, obs
        )
        contactObserver = obs
    }

    fun stopObservingContacts(context: Context) {
        contactObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        contactObserver = null
    }

    private suspend fun queryContacts(context: Context): List<ContactEntry> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ContactEntry>()
        runCatching {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
            )
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection, null, null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cur ->
                while (cur.moveToNext()) {
                    val id = cur.getLong(cur.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: continue
                    val photoUri = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                    val hasPhone = cur.getInt(cur.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0

                    val phone = if (hasPhone) {
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id.toString()), null
                        )?.use { p -> if (p.moveToFirst()) p.getString(0) else "" } ?: ""
                    } else ""

                    result += ContactEntry(id = id, displayName = name, phone = phone, photoUri = photoUri)
                }
            }
        }
        result
    }

    private fun indexContacts(entries: List<ContactEntry>) {
        contactList = entries
        val prefixIdx = HashMap<String, MutableList<ContactEntry>>()
        val trigramIdx = HashMap<String, MutableList<ContactEntry>>()
        for (entry in entries) {
            for (len in 1..minOf(4, entry.normalized.length)) {
                prefixIdx.getOrPut(entry.normalized.substring(0, len)) { mutableListOf() }.add(entry)
            }
            for (tri in entry.trigrams) {
                trigramIdx.getOrPut(tri) { mutableListOf() }.add(entry)
            }
        }
        contactPrefixIndex = prefixIdx
        contactTrigramIndex = trigramIdx
    }

    // ── Arama API ─────────────────────────────────────────────────────────────

    /**
     * Uygulamaları arar. fuzzy=true ise trigram skoru da hesaplanır.
     * sortByUsage=true ise eşit sonuçlarda usageCount'a göre sıralar.
     */
    fun searchApps(
        query: String,
        maxResults: Int = 6,
        phonetic: Boolean = true,
        fuzzy: Boolean = true,
        sortByUsage: Boolean = true
    ): List<AppInfo> {
        if (appList.isEmpty()) return emptyList()
        val q = if (phonetic) asciiFold(query.trim().lowercase()) else query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        data class Scored(val app: AppInfo, val score: Int)

        val results = HashMap<String, Scored>()

        // 1. Prefix / contains match (yüksek skor)
        for (app in appList) {
            val norm = if (phonetic) asciiFold(app.appName.lowercase()) else app.appName.lowercase()
            val score = when {
                norm == q -> 100
                norm.startsWith(q) -> 80
                norm.contains(q) -> 60
                else -> 0
            }
            if (score > 0) {
                results[app.packageName] = Scored(app, score)
            }
        }

        // 2. Fuzzy: trigram kesişim skoru
        if (fuzzy && q.length >= 2) {
            val queryTrigrams = trigramSet(q)
            val candidateCounts = HashMap<String, Int>()
            for (tri in queryTrigrams) {
                for (app in (appTrigramIndex[tri] ?: emptyList())) {
                    if (app.packageName !in results) {
                        candidateCounts[app.packageName] = (candidateCounts[app.packageName] ?: 0) + 1
                    }
                }
            }
            for ((pkg, hitCount) in candidateCounts) {
                val app = appList.firstOrNull { it.packageName == pkg } ?: continue
                val norm = if (phonetic) asciiFold(app.appName.lowercase()) else app.appName.lowercase()
                val appTrigrams = trigramSet(norm)
                val similarity = hitCount.toFloat() / maxOf(queryTrigrams.size, appTrigrams.size)
                if (similarity >= 0.35f) {
                    results[pkg] = Scored(app, (similarity * 40).toInt())
                }
            }
        }

        return results.values
            .sortedWith(
                if (sortByUsage)
                    compareByDescending<Scored> { it.score }.thenByDescending { it.app.usageCount }
                else
                    compareByDescending { it.score }
            )
            .take(maxResults)
            .map { it.app }
    }

    /**
     * Kişileri arar.
     */
    fun searchContacts(
        query: String,
        maxResults: Int = 4,
        phonetic: Boolean = true,
        fuzzy: Boolean = true
    ): List<ContactEntry> {
        if (contactList.isEmpty()) return emptyList()
        val q = if (phonetic) asciiFold(query.trim().lowercase()) else query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        data class Scored(val entry: ContactEntry, val score: Int)
        val results = HashMap<Long, Scored>()

        for (entry in contactList) {
            val norm = if (phonetic) entry.normalized else entry.displayName.lowercase()
            val score = when {
                norm == q -> 100
                norm.startsWith(q) -> 80
                norm.contains(q) -> 60
                entry.phone.contains(q) -> 50
                else -> 0
            }
            if (score > 0) results[entry.id] = Scored(entry, score)
        }

        if (fuzzy && q.length >= 2) {
            val queryTrigrams = trigramSet(q)
            val counts = HashMap<Long, Int>()
            for (tri in queryTrigrams) {
                for (e in (contactTrigramIndex[tri] ?: emptyList())) {
                    if (e.id !in results) counts[e.id] = (counts[e.id] ?: 0) + 1
                }
            }
            for ((id, hitCount) in counts) {
                val entry = contactList.firstOrNull { it.id == id } ?: continue
                val similarity = hitCount.toFloat() / maxOf(queryTrigrams.size, entry.trigrams.size)
                if (similarity >= 0.35f) results[id] = Scored(entry, (similarity * 40).toInt())
            }
        }

        return results.values
            .sortedByDescending { it.score }
            .take(maxResults)
            .map { it.entry }
    }

    fun getContactList(): List<ContactEntry> = contactList
    fun getAppList(): List<AppInfo> = appList

    // ── Yardımcı fonksiyonlar ─────────────────────────────────────────────────

    fun asciiFold(s: String): String = buildString(s.length) {
        for (ch in s) append(when (ch) {
            'ş', 'Ş' -> 's'
            'ü', 'Ü' -> 'u'
            'ö', 'Ö' -> 'o'
            'ç', 'Ç' -> 'c'
            'ğ', 'Ğ' -> 'g'
            'ı'      -> 'i'
            'İ'      -> 'i'
            else      -> ch.lowercaseChar()
        })
    }

    private fun trigramSet(s: String): Set<String> {
        if (s.length < 2) return setOf(s)
        val set = HashSet<String>()
        val padded = " $s "
        for (i in 0..padded.length - 3) set += padded.substring(i, i + 3)
        return set
    }
}
