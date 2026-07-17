package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Döngü D03 — KEY_HOME_SCORE_VISIBLE -> KEY_DIGITAL_LIFE_CARD_VISIBLE migration testleri.
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md, Döngü D03 (satır 1454-1496).
 *
 * PulseHistoryPrefsTest'teki gibi gerçek get/put/apply semantiğini görmek gerektiği için
 * mockk relaxed yerine hafif bir in-memory [SharedPreferences] sahtesi kullanılır.
 */
class AppPrefsDigitalLifeCardVisibilityTest {

    private lateinit var context: Context
    private lateinit var backingPrefs: SharedPreferences

    @Before
    fun setup() {
        backingPrefs = DigitalLifeCardFakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns backingPrefs
    }

    @Test
    fun `eski skor acikken yeni kart varsayilan olarak acik migrate edilir`() {
        backingPrefs.edit().putBoolean(AppPrefs.KEY_HOME_SCORE_VISIBLE, true).apply()

        val visible = AppPrefs.isDigitalLifeCardVisible(context)

        assertTrue(visible)
        assertTrue(backingPrefs.getBoolean("digital_life_card_visible", false))
    }

    @Test
    fun `eski skor kapaliyken yeni kart kapali migrate edilir`() {
        backingPrefs.edit().putBoolean(AppPrefs.KEY_HOME_SCORE_VISIBLE, false).apply()

        val visible = AppPrefs.isDigitalLifeCardVisible(context)

        assertFalse(visible)
        assertFalse(backingPrefs.getBoolean("digital_life_card_visible", true))
    }

    @Test
    fun `eski deger hic yoksa varsayilan acik kabul edilip migrate edilir`() {
        val visible = AppPrefs.isDigitalLifeCardVisible(context)

        assertTrue(visible)
    }

    @Test
    fun `migration bayragi ikinci cagriyi engeller`() {
        backingPrefs.edit().putBoolean(AppPrefs.KEY_HOME_SCORE_VISIBLE, true).apply()
        AppPrefs.isDigitalLifeCardVisible(context) // ilk cagri - migrate eder, true yazar

        // Kullanici yeni karti manuel kapatir
        AppPrefs.setDigitalLifeCardVisible(context, false)
        // Eski deger degismis olsa bile (orn. baska bir restore) migration tekrar calismamali
        backingPrefs.edit().putBoolean(AppPrefs.KEY_HOME_SCORE_VISIBLE, true).apply()

        val visible = AppPrefs.isDigitalLifeCardVisible(context)

        assertFalse(visible) // kullanicinin manuel kapattigi deger korunur
    }

    @Test
    fun `setDigitalLifeCardVisible dogrudan degeri yazar ve bayragi isaretler`() {
        AppPrefs.setDigitalLifeCardVisible(context, false)

        assertEquals(false, AppPrefs.isDigitalLifeCardVisible(context))
    }
}

/** Gercek [SharedPreferences] semantigini (get/put/apply) taklit eden minimal in-memory sahte. */
private class DigitalLifeCardFakeSharedPreferences : SharedPreferences {
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
