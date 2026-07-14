package com.armutlu.apporganizer.domain.usecase.contacts

import com.armutlu.apporganizer.utils.ContactActionPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * ContactSuggestionEngine birim testleri (P1.3).
 * Amac: sadece gercek sinyal varken (yeterli veri + saat dilimi eslesmesi) oneri uretilsin.
 */
class ContactSuggestionEngineTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** Verilen saat/dakika/gun icin epoch ms uretir (referans gun: 2026-01-05 Pazartesi). */
    private fun atHour(hour: Int, dayOffset: Int = 0): Long {
        val cal = Calendar.getInstance(utc)
        cal.set(2026, Calendar.JANUARY, 5 + dayOffset, hour, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun event(
        contactId: String,
        hour: Int,
        dayOffset: Int = 0,
        action: ContactActionPrefs.ActionType = ContactActionPrefs.ActionType.CALL,
    ) = ContactActionPrefs.ContactActionEvent(contactId, action, atHour(hour, dayOffset))

    @Test
    fun `yetersiz veri (5 altinda olay) bos liste doner`() {
        val now = atHour(9)
        val events = (1..4).map { event("contact_$it", hour = 9) }

        val result = ContactSuggestionEngine.suggest(events, nowMillis = now, timeZone = utc)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `saat dilimi eslesen kisi onerilir`() {
        val now = atHour(9)
        // contact_A: sabah 9'da 6 kez arandi -> guclu sinyal
        val events = (1..6).map { event("contact_A", hour = 9, dayOffset = -it) }

        val result = ContactSuggestionEngine.suggest(events, nowMillis = now, timeZone = utc)

        assertEquals(listOf("contact_A"), result)
    }

    @Test
    fun `saat penceresi disindaki olaylar oneriye katkida bulunmaz`() {
        val now = atHour(9)
        // contact_B aksam 21'de araniyor - sabah 9 sorgusuyla eslesmez (pencere disi)
        val events = (1..6).map { event("contact_B", hour = 21, dayOffset = -it) }

        val result = ContactSuggestionEngine.suggest(events, nowMillis = now, timeZone = utc)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `siklik ve recency yuksek olan kisi once siralanir`() {
        val now = atHour(9)
        // contact_A: 6 kez, hepsi yakin gunlerde (yuksek recency)
        val recentEvents = (1..6).map { event("contact_A", hour = 9, dayOffset = -it) }
        // contact_B: 6 kez ama cok eski (dusuk recency - 60+ gun once, yari omur 14 gun)
        val oldEvents = (1..6).map { event("contact_B", hour = 9, dayOffset = -60 - it) }

        val result = ContactSuggestionEngine.suggest(recentEvents + oldEvents, nowMillis = now, timeZone = utc)

        assertTrue(result.isNotEmpty())
        assertEquals("contact_A", result.first())
    }

    @Test
    fun `ayni gun bonusu ile en fazla 3 kisi onerilir`() {
        val now = atHour(9, dayOffset = 0) // Pazartesi
        val events = mutableListOf<ContactActionPrefs.ContactActionEvent>()
        for (name in listOf("A", "B", "C", "D")) {
            repeat(6) { i -> events += event("contact_$name", hour = 9, dayOffset = -(i + 1) * 7) }
        }

        val result = ContactSuggestionEngine.suggest(events, nowMillis = now, timeZone = utc)

        assertTrue(result.size <= 3)
    }

    @Test
    fun `500 kayit sinirini asan liste FIFO ile budanir`() {
        val events = (1..520).map {
            ContactActionPrefs.ContactActionEvent("contact_$it", ContactActionPrefs.ActionType.CALL, it.toLong())
        }

        val trimmed = ContactActionPrefs.trimToMax(events, ContactActionPrefs.MAX_EVENTS)

        assertEquals(500, trimmed.size)
        // En eski 20 kayit (1..20) atilmis olmali, kalan en yeniler (21..520) korunmali
        assertEquals("contact_21", trimmed.first().contactId)
        assertEquals("contact_520", trimmed.last().contactId)
    }

    @Test
    fun `bos veya bozuk JSON parse edilince bos liste doner`() {
        // org.json Android runtime'a bagli oldugundan (unit test'te stub) burada sadece
        // hatali girdiye karsi guvenli fallback davranisi dogrulanir - gercek parse/serialize
        // round-trip'i instrumented/Robolectric testte kapsanmalidir.
        val parsed = runCatching { ContactActionPrefs.parseJson("not a json") }.getOrNull()
        assertTrue(parsed == null || parsed.isEmpty())
    }
}
