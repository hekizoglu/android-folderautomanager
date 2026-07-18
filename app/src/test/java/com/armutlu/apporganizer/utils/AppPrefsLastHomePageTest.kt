package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * P00 — Döngü öncesi regresyon kilidi (roadmap hedef #8: "Son klasör sayfası preference'a
 * yazılır"). HomeScreen.kt satır ~987-991: `AppPrefs.getLastHomePage(context)` initialPage için
 * okunur, pager'ın `currentPage` Flow'u `AppPrefs.setLastHomePage(context, page)` ile her sayfa
 * değişiminde yazılır (LaunchedEffect + snapshotFlow, Compose'a bağlı — burada sadece AppPrefs
 * okuma/yazma sözleşmesi test edilir).
 *
 * Not: roadmap 2.4 "Sayfa index'i ham Int olarak saklanıyor" tespiti gerçek koda uyuyor —
 * KEY_LAST_HOME_PAGE hâlâ düz Int, semantik HomePageAnchor migration'ı henüz yapılmadı (P02+).
 * Bu test o ham-Int davranışının P00 anında fotoğrafını çeker; migration'dan sonra bu test
 * güncellenmeli veya HomePageAnchor'a taşınmalıdır.
 */
class AppPrefsLastHomePageTest {

    private lateinit var context: Context
    private lateinit var backingPrefs: SharedPreferences

    @Before
    fun setup() {
        backingPrefs = LastHomePageFakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns backingPrefs
    }

    @Test
    fun `hic yazilmamis home page varsayilan olarak 0 doner`() {
        assertEquals(0, AppPrefs.getLastHomePage(context))
    }

    @Test
    fun `setLastHomePage yazdiktan sonra getLastHomePage ayni degeri dondurur`() {
        AppPrefs.setLastHomePage(context, 3)
        assertEquals(3, AppPrefs.getLastHomePage(context))
    }

    @Test
    fun `ust uste yazilan son deger kalir`() {
        AppPrefs.setLastHomePage(context, 1)
        AppPrefs.setLastHomePage(context, 5)
        AppPrefs.setLastHomePage(context, 2)
        assertEquals(2, AppPrefs.getLastHomePage(context))
    }
}

/**
 * Gercek [SharedPreferences] semantigini (get/put/apply) taklit eden minimal in-memory sahte.
 * Not: PulseHistoryPrefsTest.kt de ayni pakette (com.armutlu.apporganizer.utils) benzer bir
 * `FakeSharedPreferences` tanimliyor — Kotlin'de ayni paketteki iki private top-level sinif ayni
 * ada sahip olamadigindan burada ayirt edici bir ad kullanildi. Tekrar birden fazla dosyada
 * ihtiyac duyulursa ortak bir test-fixtures sinifina cikarilmalidir (P00 kapsami disi).
 */
private class LastHomePageFakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            key?.let { pending[it] = values }; return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { removals.add(it) }; return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearAll = true; return this
        }
        override fun commit(): Boolean {
            apply(); return true
        }
        override fun apply() {
            if (clearAll) map.clear()
            removals.forEach { map.remove(it) }
            pending.forEach { (k, v) -> map[k] = v }
        }
    }
}
