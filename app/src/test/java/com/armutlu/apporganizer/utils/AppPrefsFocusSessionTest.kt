package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * F5 denetimi (P1): gece yarisini asan Focus Mode oturumu (orn. 23:50-00:20) eskiden 30dk'nin
 * TAMAMINI yeni gune yaziyordu. Artik endFocusSession sureyi gun sinirinda boler ve
 * getFocusMinutesToday devam eden oturumda yalniz bugunun 00:00 sonrasini sayar.
 */
class AppPrefsFocusSessionTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private lateinit var context: Context
    private lateinit var store: MutableMap<String, Any?>

    @Before
    fun setup() {
        store = mutableMapOf()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putLong(any(), any()) } answers {
            store[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg())
            editor
        }
        every { editor.apply() } answers {}

        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.edit() } returns editor
        every { prefs.getLong(any(), any()) } answers {
            (store[firstArg()] as? Long) ?: secondArg()
        }

        context = mockk(relaxed = true)
        every { context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
    }

    /** epochDay * 86_400_000 = o gunun UTC 00:00'i. */
    private fun dayStartMillis(epochDay: Long) = epochDay * 86_400_000L

    @Test
    fun `session within a single day credits that day`() {
        val day = 100L
        val start = dayStartMillis(day) + 10 * 3_600_000L // 10:00
        AppPrefs.startFocusSession(context, nowMillis = start)
        AppPrefs.endFocusSession(context, nowMillis = start + 30 * 60_000L, zoneId = zone)

        assertEquals(30L, store["focus_minutes_today_$day"])
        assertEquals(null, store["focus_minutes_today_${day + 1}"])
    }

    @Test
    fun `session crossing midnight is split between the two days`() {
        val day = 100L
        // 23:50 -> ertesi gun 00:20 (30 dk toplam: 10 dk dun + 20 dk bugun).
        val start = dayStartMillis(day + 1) - 10 * 60_000L
        val end = dayStartMillis(day + 1) + 20 * 60_000L
        AppPrefs.startFocusSession(context, nowMillis = start)
        AppPrefs.endFocusSession(context, nowMillis = end, zoneId = zone)

        assertEquals(10L, store["focus_minutes_today_$day"])
        assertEquals(20L, store["focus_minutes_today_${day + 1}"])
    }

    @Test
    fun `ongoing session started yesterday only counts today's portion in today's total`() {
        val day = 100L
        val start = dayStartMillis(day + 1) - 40 * 60_000L // dun 23:20
        store["focus_session_start_at_ms"] = start
        val now = dayStartMillis(day + 1) + 15 * 60_000L // bugun 00:15

        // Dunun 40 dakikasi bugune sayilmamali — yalniz 00:00 sonrasi 15 dk.
        assertEquals(15L, AppPrefs.getFocusMinutesToday(context, nowMillis = now, zoneId = zone))
    }

    @Test
    fun `ended midnight session makes yesterday's portion visible to yesterday not today`() {
        val day = 100L
        val start = dayStartMillis(day + 1) - 10 * 60_000L
        val end = dayStartMillis(day + 1) + 20 * 60_000L
        AppPrefs.startFocusSession(context, nowMillis = start)
        AppPrefs.endFocusSession(context, nowMillis = end, zoneId = zone)

        // Bugunun toplami yalniz bugunku 20 dk olmali.
        assertEquals(20L, AppPrefs.getFocusMinutesToday(context, nowMillis = end, zoneId = zone))
    }
}
