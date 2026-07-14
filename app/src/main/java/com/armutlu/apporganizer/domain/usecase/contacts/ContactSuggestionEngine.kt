package com.armutlu.apporganizer.domain.usecase.contacts

import com.armutlu.apporganizer.utils.ContactActionPrefs
import java.util.Calendar
import java.util.TimeZone

/**
 * P1.3 - Saat bazli kisi onerisi motoru. Saf Kotlin, Android bagimliligi yok (Context.
 * Calendar disinda -Calendar de saf JDK sinifidir, test edilebilir).
 *
 * Girdi: ContactActionPrefs.ContactActionEvent listesi (launcher icinden baslatilan
 * Ara/SMS/WhatsApp aksiyonlari - READ_CALL_LOG YOK, sadece kendi olay logumuz).
 *
 * Skor formulu (her contactId icin ayri ayri toplanir):
 *   - Saat dilimi eslesmesi: olayin saati, sorgu saatine +-1 saat penceresinde ise agirlik 3,
 *     tam ayni saatteyse ekstra +1 (toplam 4) - "simdi tam bu saatte hep bu kisiyi ariyorsun" sinyali.
 *   - Haftanin gunu eslesmesi: olay ile sorgu ayni gun (Calendar.DAY_OF_WEEK) ise +2 bonus.
 *   - Recency: exponential decay, yari omur 14 gun -> weight = 0.5^(gunFarki/14). Yeni olaylar
 *     eskilere gore daha agirlikli sayilir (kisi degisen alaskanliklarina hizli uyum saglar).
 *   - Siklik: ham eslesen olay sayisi log olceginde degil, dogrudan sayilir (frekans kendiliginden
 *     agirlik+recency ile carpildigi icin ayrica log'a gerek yok).
 *   Toplam skor = sum(saatAgirligi * gunBonusu-varsa * recencyAgirligi) tum eslesen olaylar icin.
 *
 * Yeterli veri yoksa (toplam olay < MIN_TOTAL_EVENTS veya en yuksek skor < MIN_SCORE_THRESHOLD)
 * BOS liste doner - UI bolumu hic gosterilmez (yanlis/zayif oneri sunulmaz).
 */
object ContactSuggestionEngine {

    const val MIN_TOTAL_EVENTS = 5
    private const val MIN_SCORE_THRESHOLD = 1.0
    private const val HOUR_WINDOW_WEIGHT = 3.0
    private const val EXACT_HOUR_BONUS = 1.0
    private const val SAME_DAY_BONUS = 2.0
    private const val RECENCY_HALF_LIFE_DAYS = 14.0
    private const val MAX_SUGGESTIONS = 3

    /**
     * Verilen olay listesinden, referans zaman noktasindaki (nowMillis) en olasi en fazla
     * MAX_SUGGESTIONS contactId'yi dondurur. Yetersiz veri varsa bos liste.
     */
    fun suggest(
        events: List<ContactActionPrefs.ContactActionEvent>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<String> {
        if (events.size < MIN_TOTAL_EVENTS) return emptyList()

        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val nowDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)

        val scores = HashMap<String, Double>()

        for (event in events) {
            val cal = Calendar.getInstance(timeZone).apply { timeInMillis = event.atMillis }
            val eventHour = cal.get(Calendar.HOUR_OF_DAY)
            val eventDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val hourDiff = hourDistance(eventHour, nowHour)
            if (hourDiff > 1) continue // saat penceresi disinda - katki yok

            var weight = HOUR_WINDOW_WEIGHT
            if (hourDiff == 0) weight += EXACT_HOUR_BONUS
            if (eventDayOfWeek == nowDayOfWeek) weight += SAME_DAY_BONUS

            val ageDays = (nowMillis - event.atMillis).coerceAtLeast(0L) / (24.0 * 60.0 * 60.0 * 1000.0)
            val recencyWeight = halfLifeDecay(ageDays, RECENCY_HALF_LIFE_DAYS)

            val contribution = weight * recencyWeight
            scores[event.contactId] = (scores[event.contactId] ?: 0.0) + contribution
        }

        val ranked = scores.entries
            .filter { it.value >= MIN_SCORE_THRESHOLD }
            .sortedByDescending { it.value }
            .take(MAX_SUGGESTIONS)
            .map { it.key }

        return ranked
    }

    /** Dairesel saat farki (0-23 araliginda), orn. 23 ile 0 arasi fark 1 olmali. */
    private fun hourDistance(a: Int, b: Int): Int {
        val diff = Math.abs(a - b)
        return minOf(diff, 24 - diff)
    }

    /** Exponential decay: halfLifeDays gun sonra agirlik 0.5 olur. */
    private fun halfLifeDecay(ageDays: Double, halfLifeDays: Double): Double =
        Math.pow(0.5, ageDays / halfLifeDays)
}
