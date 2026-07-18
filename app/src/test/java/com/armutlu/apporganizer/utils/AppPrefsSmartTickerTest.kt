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
 * Döngü T05 — Akıllı Nabız ayarları testleri
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1848-1905).
 *
 * Kapsam:
 * - KEY_TICKER_ENABLED -> KEY_SMART_TICKER_ENABLED migration (AppPrefsDigitalLifeCardVisibilityTest
 *   ile aynı desen — gerçek get/put/apply semantiği için hafif in-memory sahte SharedPreferences).
 * - isSmartTickerTypeVisible() tür-grubu eşlemesi (MISSION_PROGRESS + MISSION_ACHIEVEMENT tek
 *   switch altında birleşir, roadmap mock satır 1859 "Görev uyarıları ve başarılar").
 * - Hidden-types add/remove simetrisi (T04 tekil kapatma + T05 toplu ayar ekranı birlikte çalışır).
 */
class AppPrefsSmartTickerTest {

    private lateinit var context: Context
    private lateinit var backingPrefs: SharedPreferences

    @Before
    fun setup() {
        backingPrefs = SmartTickerFakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns backingPrefs
    }

    // ── Migration: KEY_TICKER_ENABLED -> KEY_SMART_TICKER_ENABLED ──────────

    @Test
    fun `eski ticker acikken yeni anahtar acik migrate edilir`() {
        @Suppress("DEPRECATION")
        backingPrefs.edit().putBoolean(AppPrefs.KEY_TICKER_ENABLED, true).apply()

        val enabled = AppPrefs.isTickerEnabled(context)

        assertTrue(enabled)
        assertTrue(backingPrefs.getBoolean(AppPrefs.KEY_SMART_TICKER_ENABLED, false))
    }

    @Test
    fun `eski ticker kapaliyken yeni anahtar kapali migrate edilir`() {
        @Suppress("DEPRECATION")
        backingPrefs.edit().putBoolean(AppPrefs.KEY_TICKER_ENABLED, false).apply()

        val enabled = AppPrefs.isTickerEnabled(context)

        assertFalse(enabled)
        assertFalse(backingPrefs.getBoolean(AppPrefs.KEY_SMART_TICKER_ENABLED, true))
    }

    @Test
    fun `eski deger hic yoksa varsayilan acik kabul edilir`() {
        val enabled = AppPrefs.isTickerEnabled(context)

        assertTrue(enabled)
    }

    @Test
    fun `migration bayragi ikinci cagriyi engeller`() {
        @Suppress("DEPRECATION")
        backingPrefs.edit().putBoolean(AppPrefs.KEY_TICKER_ENABLED, true).apply()
        AppPrefs.isTickerEnabled(context) // ilk cagri - migrate eder

        AppPrefs.setTickerEnabled(context, false) // kullanici manuel kapatir
        @Suppress("DEPRECATION")
        backingPrefs.edit().putBoolean(AppPrefs.KEY_TICKER_ENABLED, true).apply() // eski deger tekrar true olsa da

        assertFalse(AppPrefs.isTickerEnabled(context)) // kullanicinin manuel tercihi korunur
    }

    // ── isSmartTickerTypeVisible — tür grubu eşlemesi ───────────────────────

    @Test
    fun `gorev turleri tek switch altinda birlesir - ikisi de acik varsayilan`() {
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "MISSION_PROGRESS"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "MISSION_ACHIEVEMENT"))
    }

    @Test
    fun `gorev switch kapatilinca hem ilerleme hem basari turu gizlenir`() {
        AppPrefs.setSmartTickerMissionsVisible(context, false)

        assertFalse(AppPrefs.isSmartTickerTypeVisible(context, "MISSION_PROGRESS"))
        assertFalse(AppPrefs.isSmartTickerTypeVisible(context, "MISSION_ACHIEVEMENT"))
        // Diger turler etkilenmez (kullanici bir turu kapatirken digerini kaybetmemeli - kabul kriteri)
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "ACTION_REQUIRED"))
    }

    @Test
    fun `ozellik ipuclari varsayilan kapali - roadmap mock satir 1863`() {
        assertFalse(AppPrefs.isSmartTickerTypeVisible(context, "FEATURE_DISCOVERY"))
        assertFalse(AppPrefs.isSmartTickerDiscoveryVisible(context))
    }

    @Test
    fun `diger 6 tur varsayilan acik`() {
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "ACTION_REQUIRED"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "PULSE_CHANGE"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "WEEKLY_REPORT"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "CONTEXTUAL_SUGGESTION"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "CRITICAL_HEALTH"))
    }

    @Test
    fun `bilinmeyen tur adi guvenli varsayilan olarak gorunur kabul edilir`() {
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "UNKNOWN_FUTURE_TYPE"))
    }

    @Test
    fun `tek tur kapatilinca digerleri acik kalir - kabul kriteri`() {
        AppPrefs.setSmartTickerPulseVisible(context, false)

        assertFalse(AppPrefs.isSmartTickerTypeVisible(context, "PULSE_CHANGE"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "ACTION_REQUIRED"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "WEEKLY_REPORT"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "CONTEXTUAL_SUGGESTION"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "CRITICAL_HEALTH"))
        assertTrue(AppPrefs.isSmartTickerTypeVisible(context, "MISSION_PROGRESS"))
    }

    // ── Hidden types add/remove simetrisi (T04 + T05) ───────────────────────

    @Test
    fun `hidden type eklenip kaldirilinca sete geri girmez`() {
        AppPrefs.addTickerHiddenType(context, "WEEKLY_REPORT")
        assertTrue("WEEKLY_REPORT" in AppPrefs.getTickerHiddenTypes(context))

        AppPrefs.removeTickerHiddenType(context, "WEEKLY_REPORT")
        assertFalse("WEEKLY_REPORT" in AppPrefs.getTickerHiddenTypes(context))
    }

    @Test
    fun `remove sirasinda diger hidden turler etkilenmez`() {
        AppPrefs.addTickerHiddenType(context, "WEEKLY_REPORT")
        AppPrefs.addTickerHiddenType(context, "FEATURE_DISCOVERY")

        AppPrefs.removeTickerHiddenType(context, "WEEKLY_REPORT")

        val remaining = AppPrefs.getTickerHiddenTypes(context)
        assertFalse("WEEKLY_REPORT" in remaining)
        assertTrue("FEATURE_DISCOVERY" in remaining)
    }

    // ── Otomatik geçiş / interval / hassas bilgi ────────────────────────────

    @Test
    fun `interval varsayilan 10 saniye ve 5-20 araligina sikistirilir`() {
        assertEquals(10, AppPrefs.getTickerIntervalSeconds(context))

        AppPrefs.setTickerIntervalSeconds(context, 45)
        assertEquals(20, AppPrefs.getTickerIntervalSeconds(context))

        AppPrefs.setTickerIntervalSeconds(context, 1)
        assertEquals(5, AppPrefs.getTickerIntervalSeconds(context))
    }

    @Test
    fun `hassas bilgi varsayilan kapali - gizlilik onceligi`() {
        assertFalse(AppPrefs.isTickerSensitiveVisible(context))

        AppPrefs.setTickerSensitiveVisible(context, true)
        assertTrue(AppPrefs.isTickerSensitiveVisible(context))
    }

    @Test
    fun `otomatik gecis varsayilan acik`() {
        assertTrue(AppPrefs.isTickerAutoAdvanceEnabled(context))

        AppPrefs.setTickerAutoAdvanceEnabled(context, false)
        assertFalse(AppPrefs.isTickerAutoAdvanceEnabled(context))
    }

    @Test
    fun `sessizlik erken kaldirilinca sifirlanir`() {
        AppPrefs.setTickerMutedUntil(context, System.currentTimeMillis() + 60_000L)
        assertTrue(AppPrefs.getTickerMutedUntil(context) > 0L)

        AppPrefs.clearTickerMutedUntil(context)
        assertEquals(0L, AppPrefs.getTickerMutedUntil(context))
    }
}

/** Gercek [SharedPreferences] semantigini (get/put/apply) taklit eden minimal in-memory sahte. */
private class SmartTickerFakeSharedPreferences : SharedPreferences {
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
