package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import java.time.Clock
import java.time.ZoneId
import timber.log.Timber

/**
 * Döngü G4 (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md) — Streak (Seri) Sistemi saklaması.
 * [PulseHistoryPrefs] deseniyle birebir aynı yaklaşım: SharedPreferences, ISO gün/hafta
 * anahtarlı, Room GEREKMEZ.
 *
 * Kavramlar:
 * - **Günlük seri (currentStreak):** en az 1 görev tamamlanan ardışık gün sayısı.
 * - **En iyi seri (bestStreak):** şimdiye kadar ulaşılan en yüksek currentStreak (asla azalmaz).
 * - **Altın seri (goldenStreak):** günün TÜM görevleri (3/3) tamamlandığı ardışık gün sayısı —
 *   currentStreak'ten AYRI bir sayaç (bir gün 1/3 yapılırsa currentStreak devam eder ama
 *   goldenStreak sıfırlanır; roadmap G4 "3/3 günler ayrı sayaç").
 * - **Dondurma hakkı (freeze):** haftada 1 kez, 1 günlük boşluğu affeder — seri kırılmaz, o gün
 *   "donduruldu" olarak işaretlenir. Hak ISO haftaya göre yenilenir (haftalık anahtar).
 *
 * Ceza YOK ilkesi (M08 ile aynı ilke, roadmap G4): seri kırıldığında negatif dil/ceza yok,
 * yalnızca nazik sıfırlama — "yeni seri bugün başlıyor" gibi.
 *
 * Saklama şeması (SharedPreferences, dosya: "wrapped_prefs" — PulseHistoryPrefs ile paylaşılır,
 * aynı prefs dosyasında farklı anahtar öneki kullanmak PulseHistoryPrefs deseniyle tutarlıdır):
 * - `mission_streak_current` (Int): mevcut ardışık gün sayısı.
 * - `mission_streak_best` (Int): şimdiye kadarki en yüksek currentStreak.
 * - `mission_streak_golden` (Int): mevcut ardışık "3/3 tamamlanan gün" sayısı.
 * - `mission_streak_last_counted_epoch_day` (Long): son kez [advance] ile SAYILAN (idempotency
 *   anahtarı) epochDay — aynı gün ikinci kez çağrılırsa hiçbir alan değişmez.
 * - `mission_streak_last_frozen_epoch_day` (Long): son dondurulan günün epochDay'i (-1 = hiç
 *   dondurulmadı) — UI "Dün seri donduruldu ❄" bilgisini bu alana bakarak gösterir.
 * - `mission_streak_freeze_week` (Long): dondurma hakkının ait olduğu ISO hafta anahtarı
 *   (weekStartEpochDay) — [PeriodBoundaryResolver.currentIsoWeek] ile üretilir (PulseHistoryPrefs
 *   ile aynı hafta tanımı).
 * - `mission_streak_freeze_used_this_week` (Boolean): o haftanın dondurma hakkı kullanıldı mı.
 */
object MissionStreakPrefs {

    private const val PREFS_NAME = "wrapped_prefs"

    private const val KEY_CURRENT = "mission_streak_current"
    private const val KEY_BEST = "mission_streak_best"
    private const val KEY_GOLDEN = "mission_streak_golden"
    private const val KEY_LAST_COUNTED_EPOCH_DAY = "mission_streak_last_counted_epoch_day"
    private const val KEY_LAST_FROZEN_EPOCH_DAY = "mission_streak_last_frozen_epoch_day"
    private const val KEY_FREEZE_WEEK = "mission_streak_freeze_week"
    private const val KEY_FREEZE_USED_THIS_WEEK = "mission_streak_freeze_used_this_week"

    private const val NO_EPOCH_DAY = -1L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Anlık, saklanmış seri durumu — UI'nin doğrudan okuyabileceği salt-okunur görünüm.
     */
    data class StreakState(
        val currentStreak: Int,
        val bestStreak: Int,
        val goldenStreak: Int,
        val lastCountedEpochDay: Long,
        val lastFrozenEpochDay: Long,
    ) {
        /** UI "Dün seri donduruldu ❄" bilgisini göstermeli mi — [todayEpochDay] - 1 dondurulduysa. */
        fun wasFrozenYesterday(todayEpochDay: Long): Boolean =
            lastFrozenEpochDay != NO_EPOCH_DAY && lastFrozenEpochDay == todayEpochDay - 1
    }

    /** Saklanmış seri durumunu okur — hiçbir yan etkisi yoktur (advance ÇAĞIRMAZ). */
    fun read(context: Context): StreakState {
        val p = prefs(context)
        return StreakState(
            currentStreak = p.getInt(KEY_CURRENT, 0),
            bestStreak = p.getInt(KEY_BEST, 0),
            goldenStreak = p.getInt(KEY_GOLDEN, 0),
            lastCountedEpochDay = p.getLong(KEY_LAST_COUNTED_EPOCH_DAY, NO_EPOCH_DAY),
            lastFrozenEpochDay = p.getLong(KEY_LAST_FROZEN_EPOCH_DAY, NO_EPOCH_DAY),
        )
    }

    /**
     * Bir günün settlement'ı kapanınca çağrılır (bkz. SettleMissionInstancesUseCase entegrasyonu).
     * [epochDay] o günün epoch-day'i, [completedCount]/[totalCount] o günün settlement SONUCU
     * (kaç görev COMPLETED oldu / toplam kaç görev vardı).
     *
     * Idempotent: [epochDay] daha önce zaten sayıldıysa ([StreakState.lastCountedEpochDay] ile
     * aynıysa) hiçbir şey değişmez, mevcut durum aynen döner.
     *
     * @param weekStartEpochDayForFreezeCheck [epochDay]'in ait olduğu ISO haftanın Pazartesi
     * epochDay'i — dondurma hakkının haftalık yenilenmesi için (PeriodBoundaryResolver'dan
     * üretilir, çağıran taraf sorumludur — saf fonksiyon Android'e bağımlı olmasın diye burada
     * hesaplanmaz).
     */
    fun advance(
        context: Context,
        epochDay: Long,
        completedCount: Int,
        totalCount: Int,
        weekStartEpochDayForFreezeCheck: Long,
    ): StreakState {
        return runCatching {
            val p = prefs(context)
            val before = read(context)

            // Idempotency: bu gün zaten sayıldıysa hiçbir şey değişmesin.
            if (before.lastCountedEpochDay == epochDay) return before

            val result = advancePure(
                before = PureStreakState(
                    currentStreak = before.currentStreak,
                    bestStreak = before.bestStreak,
                    goldenStreak = before.goldenStreak,
                    lastCountedEpochDay = before.lastCountedEpochDay,
                    lastFrozenEpochDay = before.lastFrozenEpochDay,
                    freezeWeek = p.getLong(KEY_FREEZE_WEEK, NO_EPOCH_DAY),
                    freezeUsedThisWeek = p.getBoolean(KEY_FREEZE_USED_THIS_WEEK, false),
                ),
                epochDay = epochDay,
                completedCount = completedCount,
                totalCount = totalCount,
                weekStartEpochDay = weekStartEpochDayForFreezeCheck,
            )

            p.edit()
                .putInt(KEY_CURRENT, result.currentStreak)
                .putInt(KEY_BEST, result.bestStreak)
                .putInt(KEY_GOLDEN, result.goldenStreak)
                .putLong(KEY_LAST_COUNTED_EPOCH_DAY, result.lastCountedEpochDay)
                .putLong(KEY_LAST_FROZEN_EPOCH_DAY, result.lastFrozenEpochDay)
                .putLong(KEY_FREEZE_WEEK, result.freezeWeek)
                .putBoolean(KEY_FREEZE_USED_THIS_WEEK, result.freezeUsedThisWeek)
                .apply()

            StreakState(
                currentStreak = result.currentStreak,
                bestStreak = result.bestStreak,
                goldenStreak = result.goldenStreak,
                lastCountedEpochDay = result.lastCountedEpochDay,
                lastFrozenEpochDay = result.lastFrozenEpochDay,
            )
        }.onFailure { e -> Timber.e(e, "MissionStreakPrefs.advance basarisiz") }
            .getOrDefault(read(context))
    }

    /** P0.4 istatistik sıfırlama sihirbazı ile uyumlu — seri geçmişini tamamen temizler. */
    fun clearAll(context: Context) {
        prefs(context).edit()
            .remove(KEY_CURRENT)
            .remove(KEY_BEST)
            .remove(KEY_GOLDEN)
            .remove(KEY_LAST_COUNTED_EPOCH_DAY)
            .remove(KEY_LAST_FROZEN_EPOCH_DAY)
            .remove(KEY_FREEZE_WEEK)
            .remove(KEY_FREEZE_USED_THIS_WEEK)
            .apply()
    }

    // ---- Saf iç model + saf fonksiyon (unit test edilebilir, Android bağımlılığı yok) ----

    internal data class PureStreakState(
        val currentStreak: Int,
        val bestStreak: Int,
        val goldenStreak: Int,
        val lastCountedEpochDay: Long,
        val lastFrozenEpochDay: Long,
        val freezeWeek: Long,
        val freezeUsedThisWeek: Boolean,
    )

    /**
     * Saf güncelleme kuralı (roadmap G4, sadeleştirilmiş — "ödül/ceza yok" ilkesiyle):
     * - `completedCount >= 1` DEĞİLSE (bugün hiç görev tamamlanmadı) seri bu gün için İLERLEMEZ —
     *   [lastCountedEpochDay] yine de bu güne güncellenir (idempotency), ama currentStreak/
     *   golden DEĞİŞMEZ (o günün "boş" olduğu daha sonra [advance] gap hesaplamasında görülür).
     *   NOT: roadmap'te "ardışık gün" tanımı >=1 tamamlanan görev — bu yüzden 0 tamamlanan gün
     *   "boşluk günü" sayılır, ceza uygulanmaz, sadece ilerleme durur.
     * - Ardışık gün (epochDay == lastCountedEpochDay + 1, önceki gün sayılmıştı ve o gün
     *   tamamlanmıştı) → currentStreak + 1.
     * - 1 günlük boşluk (epochDay == lastCountedEpochDay + 2) VE bu hafta dondurma hakkı
     *   kullanılmamışsa → hak otomatik harcanır, ARADAKİ gün "donduruldu" sayılır, seri KORUNUR
     *   ve bugün tamamlandıysa +1 ile devam eder.
     * - 2+ gün boşluk VEYA hak yoksa → seri KIRILIR, ceza/negatif YOK — bugün tamamlandıysa yeni
     *   seri 1'den başlar, tamamlanmadıysa 0'dır.
     * - Golden seri (3/3): completedCount == totalCount && totalCount > 0 olan ardışık günler
     *   ayrı sayılır — currentStreak'in ardışıklık kuralına tabi değildir, sadece "bir önceki
     *   sayılan gün de golden mıydı" kontrolüyle ilerler/sıfırlanır.
     * - Hafta değişince ([weekStartEpochDay] != [PureStreakState.freezeWeek]) dondurma hakkı
     *   tazelenir (freezeUsedThisWeek = false).
     */
    internal fun advancePure(
        before: PureStreakState,
        epochDay: Long,
        completedCount: Int,
        totalCount: Int,
        weekStartEpochDay: Long,
    ): PureStreakState {
        // Haftalık dondurma hakkı yenilemesi.
        val freezeWeek: Long
        val freezeUsedThisWeek: Boolean
        if (before.freezeWeek != weekStartEpochDay) {
            freezeWeek = weekStartEpochDay
            freezeUsedThisWeek = false
        } else {
            freezeWeek = before.freezeWeek
            freezeUsedThisWeek = before.freezeUsedThisWeek
        }

        val todayCompleted = completedCount >= 1
        val todayGolden = totalCount > 0 && completedCount == totalCount

        // İlk kayıt (hiç sayılmamış gün yok) — bugün baz alınır.
        if (before.lastCountedEpochDay == NO_EPOCH_DAY) {
            return before.copy(
                currentStreak = if (todayCompleted) 1 else 0,
                bestStreak = maxOf(before.bestStreak, if (todayCompleted) 1 else 0),
                goldenStreak = if (todayGolden) 1 else 0,
                lastCountedEpochDay = epochDay,
                freezeWeek = freezeWeek,
                freezeUsedThisWeek = freezeUsedThisWeek,
            )
        }

        val gap = epochDay - before.lastCountedEpochDay

        return when {
            // Ardışık gün — önceki gün sayılmıştı, boşluk yok.
            gap == 1L -> {
                val newCurrent = if (todayCompleted) before.currentStreak + 1 else 0
                val newGolden = if (todayGolden) before.goldenStreak + 1 else 0
                before.copy(
                    currentStreak = newCurrent,
                    bestStreak = maxOf(before.bestStreak, newCurrent),
                    goldenStreak = newGolden,
                    lastCountedEpochDay = epochDay,
                    freezeWeek = freezeWeek,
                    freezeUsedThisWeek = freezeUsedThisWeek,
                )
            }
            // 1 günlük boşluk + dondurma hakkı müsait → hak harcanır, seri korunur.
            gap == 2L && !freezeUsedThisWeek -> {
                val frozenDay = before.lastCountedEpochDay + 1
                val newCurrent = if (todayCompleted) before.currentStreak + 1 else before.currentStreak
                val newGolden = if (todayGolden) before.goldenStreak + 1 else 0
                before.copy(
                    currentStreak = newCurrent,
                    bestStreak = maxOf(before.bestStreak, newCurrent),
                    goldenStreak = newGolden,
                    lastCountedEpochDay = epochDay,
                    lastFrozenEpochDay = frozenDay,
                    freezeWeek = freezeWeek,
                    freezeUsedThisWeek = true,
                )
            }
            // 2+ gün boşluk VEYA hak yok — nazik sıfırlama, ceza yok.
            else -> {
                val newCurrent = if (todayCompleted) 1 else 0
                val newGolden = if (todayGolden) 1 else 0
                before.copy(
                    currentStreak = newCurrent,
                    bestStreak = maxOf(before.bestStreak, newCurrent),
                    goldenStreak = newGolden,
                    lastCountedEpochDay = epochDay,
                    freezeWeek = freezeWeek,
                    freezeUsedThisWeek = freezeUsedThisWeek,
                )
            }
        }
    }
}
