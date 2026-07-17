package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Döngü D01 — PulseHistoryPrefs.updateCurrentWeekScore() trend/baseline mantığı testleri.
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md, Döngü D01 (satır 1311-1356).
 *
 * Bu projede SharedPreferences testleri Robolectric OLMADAN yapılır (bkz.
 * StatsResetServiceTest.kt) — burada gerçek okuma/yazma semantiğini (get sonrası apply'i
 * görmek) doğrulamak gerektiği için mockk relaxed yerine hafif bir in-memory
 * [SharedPreferences] sahtesi kullanılır.
 */
class PulseHistoryPrefsTest {

    private lateinit var context: Context
    private lateinit var backingPrefs: SharedPreferences

    @Before
    fun setup() {
        backingPrefs = FakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns backingPrefs
    }

    // Haftalar (ISO Pazartesi epoch-day, roadmap H01 ile tutarlı):
    // 2026-07-13 Pazartesi = 20647L (referans değer, testte doğrudan LocalDate'ten türetilir).
    private val week1 = java.time.LocalDate.of(2026, 7, 13).toEpochDay() // Pazartesi
    private val week2 = week1 + 7 // bir sonraki Pazartesi
    private val week4 = week1 + 21 // 3 hafta atlanmış (week2, week3 hiç açılmamış)

    @Test
    fun `ilk hafta baseline yok - delta null`() {
        val result = PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 70)

        assertNull(result.previousScore)
        assertNull(result.scoreDelta)
    }

    @Test
    fun `ayni hafta icinde tekrar cagrilinca baseline degismez`() {
        PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 70)
        val second = PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 75)
        val third = PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 60)

        // Hala ilk hafta - hicbir kapanmis hafta yok, previousScore hep null kalmali.
        assertNull(second.previousScore)
        assertNull(third.previousScore)
    }

    @Test
    fun `yeni pazartesi onceki haftanin son running skorunu kapanis olarak doner`() {
        PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 70)
        PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 72) // hafta icinde guncelleme (son running=72)

        val result = PulseHistoryPrefs.updateCurrentWeekScore(context, week2, 76)

        assertEquals(72, result.previousScore)
        assertEquals(4, result.scoreDelta) // 76 - 72
    }

    @Test
    fun `ikinci haftada da baseline sabit kalir`() {
        PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 70)
        PulseHistoryPrefs.updateCurrentWeekScore(context, week2, 76)

        val second = PulseHistoryPrefs.updateCurrentWeekScore(context, week2, 80)
        val third = PulseHistoryPrefs.updateCurrentWeekScore(context, week2, 65)

        assertEquals(70, second.previousScore)
        assertEquals(70, third.previousScore)
    }

    @Test
    fun `hafta atlanirsa en son kapanmis haftanin skoru kullanilir`() {
        PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 70) // week1 running=70
        // week2 ve week3 hic acilmadi - dogrudan week4'e atlaniyor.
        val result = PulseHistoryPrefs.updateCurrentWeekScore(context, week4, 90)

        // week1 kapanisa donusur (70), week4'un karsilastirmasi bu olur.
        assertEquals(70, result.previousScore)
        assertEquals(20, result.scoreDelta)
    }

    @Test
    fun `zaman dilimi degisimi haftayi bozmaz - weekStartEpochDay tek kaynak`() {
        // PeriodBoundaryResolver farkli zone'larda farkli currentDay uretebilir ama
        // weekStartEpochDay ayni Pazartesi'yi temsil ettigi surece PulseHistoryPrefs
        // sadece bu deger uzerinden calisir - zone bilgisi burada YOKTUR.
        PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 70)
        val sameWeekDifferentCall = PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 71)

        assertNull(sameWeekDifferentCall.previousScore)
    }

    @Test
    fun `eski D244 verisi varsa mevcut haftanin running baseline'i olarak tasinir`() {
        // Eski format: pulse_week_score=68, pulse_week_day=... (gun bazli, artik kullanilmiyor).
        backingPrefs.edit()
            .putInt("pulse_week_score", 68)
            .putLong("pulse_week_day", 19000L)
            .apply()

        // Migration ilk cagrida calisir - week1 running=68 olarak baslar (henuz kapanmamis).
        val first = PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 74)
        assertNull(first.previousScore) // week1 hala running - kapanmis hafta yok.

        // week2'ye gecince week1'in running'i (74, migration sonrasi update ile ezildi) kapanir.
        val second = PulseHistoryPrefs.updateCurrentWeekScore(context, week2, 80)
        assertEquals(74, second.previousScore)
    }

    @Test
    fun `bozuk eski veri varsa temiz baslar`() {
        // Legacy skor yok (varsayilan -1) - migration okunamiyor, temiz baslamali.
        val result = PulseHistoryPrefs.updateCurrentWeekScore(context, week1, 55)
        assertNull(result.previousScore)
    }
}

/** Gercek [SharedPreferences] semantigini (get/put/apply) taklit eden minimal in-memory sahte. */
private class FakeSharedPreferences : SharedPreferences {
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
