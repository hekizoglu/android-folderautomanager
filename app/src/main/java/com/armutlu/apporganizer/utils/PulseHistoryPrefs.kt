package com.armutlu.apporganizer.utils

import android.content.Context
import timber.log.Timber

/**
 * Döngü D01 — Dijital Nabız skoru için ISO takvim haftasına dayanan trend/baseline saklaması.
 * Roadmap: ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md, Döngü D01 (satır 1311-1356).
 *
 * Eski `WrappedSnapshotPrefs.updateWeeklyPulseScore()` 7 günlük bir rotasyon kullanıyordu — bu,
 * gerçek Pazartesi-Pazar ISO haftası sınırıyla birebir örtüşmüyordu (örn. Salı günü açılan uygulama
 * bir sonraki Salı'ya kadar "aynı hafta" sayılıyordu, takvimdeki Pazartesi geçişini kaçırıyordu).
 *
 * Yeni model: her ISO haftanın anahtarı [PeriodBoundaryResolver.currentIsoWeek] ile üretilen
 * `weekStartEpochDay` (o haftanın Pazartesi'sinin epoch-day'i). Mevcut hafta için "running" (o ana
 * kadar en son hesaplanan) skor bu anahtar altında güncellenir; hafta değiştiğinde eski "running"
 * değer otomatik olarak kapanış (closing) skoru hâline gelir — ayrı bir "kapat" adımı gerekmez.
 *
 * Saklama şeması (SharedPreferences, dosya: "wrapped_prefs" — mevcut dosyayla paylaşılır):
 * - `pulse_history_running_week` (Long): üzerinde çalışılan haftanın weekStartEpochDay'i.
 * - `pulse_history_running_score` (Int): o haftanın en son hesaplanan (running) skoru.
 * - `pulse_history_<weekStartEpochDay>` (Int): KAPANMIŞ bir haftanın son skoru — hafta değişince
 *   önceki running değer bu anahtara yazılır. Sınırsız birikmez; sadece geçmişe dönük okuma için
 *   en fazla [MAX_RETAINED_WEEKS] hafta saklanır (eski haftalar temizlenir).
 * - `pulse_history_migrated` (Boolean): eski `KEY_PULSE_WEEK_*` verisinin bir kez taşındığını işaretler.
 */
object PulseHistoryPrefs {

    private const val PREFS_NAME = "wrapped_prefs"

    private const val KEY_RUNNING_WEEK = "pulse_history_running_week"
    private const val KEY_RUNNING_SCORE = "pulse_history_running_score"
    private const val KEY_CLOSED_WEEK_PREFIX = "pulse_history_"
    private const val KEY_CLOSED_WEEKS_INDEX = "pulse_history_closed_weeks_index" // virgülle ayrılmış weekStartEpochDay listesi
    private const val KEY_MIGRATED = "pulse_history_migrated"

    // Eski (D244) anahtarlar — sadece tek seferlik migration okuması için referans edilir.
    private const val LEGACY_KEY_PULSE_WEEK_SCORE = "pulse_week_score"
    private const val LEGACY_KEY_PULSE_WEEK_DAY = "pulse_week_day"

    private const val MAX_RETAINED_WEEKS = 8

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Bu haftanın (roadmap ISO hafta tanımı) skorunu günceller ve trend karşılaştırması için
     * `PulseTrendResult` döndürür.
     *
     * Kurallar (roadmap D01):
     * - Mevcut hafta içinde BASELINE değişmez — sadece "running" skor güncellenir, karşılaştırma
     *   değeri (kapanmış önceki hafta skoru) sabit kalır.
     * - Hafta değiştiğinde: eski running skor o haftanın kapanışı olarak history'ye yazılır,
     *   yeni hafta running olarak başlatılır.
     * - İlk hafta (hiç geçmiş yok) → previousScore = null ("Veri birikiyor").
     * - Bir veya daha fazla hafta hiç açılmadan atlanmışsa (running week ile şimdiki hafta arasında
     *   boşluk varsa) → en son kapanmış haftanın skoru karşılaştırma olarak kullanılır (en güncel
     *   bilinen veri, null DEĞİL) — roadmap D01 "rotasyon mantığı" sorusunun kararı: haftalar
     *   atlansa da elimizdeki en taze kapanış anlamlıdır, kullanıcıyı sıfırdan başlatmaya gerek yok.
     */
    fun updateCurrentWeekScore(context: Context, currentWeekStartEpochDay: Long, score: Int): PulseTrendResult {
        return runCatching {
            migrateLegacyIfNeeded(context, currentWeekStartEpochDay)

            val p = prefs(context)
            val runningWeek = p.getLong(KEY_RUNNING_WEEK, -1L)
            val runningScore = p.getInt(KEY_RUNNING_SCORE, -1)

            val editor = p.edit()
            when {
                runningWeek < 0L -> {
                    // İlk çalıştırma — bu hafta running olarak başlar, karşılaştırma yok.
                    editor.putLong(KEY_RUNNING_WEEK, currentWeekStartEpochDay)
                    editor.putInt(KEY_RUNNING_SCORE, score)
                    editor.apply()
                }
                runningWeek == currentWeekStartEpochDay -> {
                    // Aynı hafta içindeyiz — baseline (kapanmış önceki hafta) DEĞİŞMEZ,
                    // sadece bu haftanın running skorunu güncelle.
                    editor.putInt(KEY_RUNNING_SCORE, score)
                    editor.apply()
                }
                else -> {
                    // Hafta değişti (1 veya daha fazla hafta atlanmış olabilir) — eski running
                    // skor, ait olduğu haftanın KAPANIŞI olarak history'ye yazılır.
                    writeClosedWeek(p, editor, runningWeek, runningScore.takeIf { it in 0..100 })
                    editor.putLong(KEY_RUNNING_WEEK, currentWeekStartEpochDay)
                    editor.putInt(KEY_RUNNING_SCORE, score)
                    editor.apply()
                }
            }

            val previousScore = latestClosedScoreBefore(prefs(context), currentWeekStartEpochDay)
            PulseTrendResult(
                previousScore = previousScore,
                scoreDelta = previousScore?.let { score - it },
            )
        }.onFailure { e -> Timber.e(e, "PulseHistoryPrefs.updateCurrentWeekScore basarisiz") }
            .getOrDefault(PulseTrendResult(previousScore = null, scoreDelta = null))
    }

    /** En son KAPANMIŞ haftanın skorunu (varsa) — [beforeWeekStartEpochDay]'den kesinlikle önceki. */
    private fun latestClosedScoreBefore(p: android.content.SharedPreferences, beforeWeekStartEpochDay: Long): Int? {
        val closedWeeks = closedWeekIndex(p).filter { it < beforeWeekStartEpochDay }
        val latestWeek = closedWeeks.maxOrNull() ?: return null
        val v = p.getInt(closedWeekKey(latestWeek), -1)
        return v.takeIf { it in 0..100 }
    }

    private fun writeClosedWeek(
        p: android.content.SharedPreferences,
        editor: android.content.SharedPreferences.Editor,
        weekStartEpochDay: Long,
        score: Int?,
    ) {
        if (score == null) return
        editor.putInt(closedWeekKey(weekStartEpochDay), score)

        val updatedIndex = (closedWeekIndex(p) + weekStartEpochDay)
            .distinct()
            .sortedDescending()
            .take(MAX_RETAINED_WEEKS)

        // Retention dışına düşen eski haftaların skor anahtarlarını temizle (sınırsız birikmesin).
        val dropped = closedWeekIndex(p).filter { it !in updatedIndex }
        dropped.forEach { editor.remove(closedWeekKey(it)) }

        editor.putString(KEY_CLOSED_WEEKS_INDEX, updatedIndex.joinToString(","))
    }

    private fun closedWeekKey(weekStartEpochDay: Long): String = "$KEY_CLOSED_WEEK_PREFIX$weekStartEpochDay"

    private fun closedWeekIndex(p: android.content.SharedPreferences): List<Long> {
        val raw = p.getString(KEY_CLOSED_WEEKS_INDEX, null) ?: return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    /**
     * Eski `WrappedSnapshotPrefs.KEY_PULSE_WEEK_SCORE/DAY` (D244 haftalık rotasyon) verisi
     * varsa, YENİ şemaya bir kez taşınır — kontrollü migration:
     * - Eski veri okunabiliyorsa (skor 0..100, gün >= 0): mevcut haftanın "running" baseline'ı
     *   olarak taşınır (eski değer zaten "en son hesaplanan skor" anlamına geliyordu).
     * - Okunamıyorsa (yok/bozuk): temiz başlanır, migration yine de işaretlenir (tekrar denenmez).
     * Bu fonksiyon idempotent'tir — `KEY_MIGRATED` bayrağı ile tek seferlik çalışır.
     */
    private fun migrateLegacyIfNeeded(context: Context, currentWeekStartEpochDay: Long) {
        val p = prefs(context)
        if (p.getBoolean(KEY_MIGRATED, false)) return
        if (p.getLong(KEY_RUNNING_WEEK, -1L) >= 0L) {
            // Yeni şemada zaten veri var (örn. testte doğrudan yazılmış) — migration'ı atla ama işaretle.
            p.edit().putBoolean(KEY_MIGRATED, true).apply()
            return
        }

        val legacyScore = p.getInt(LEGACY_KEY_PULSE_WEEK_SCORE, -1)
        val legacyDay = p.getLong(LEGACY_KEY_PULSE_WEEK_DAY, -1L)

        val editor = p.edit()
        if (legacyScore in 0..100 && legacyDay >= 0L) {
            // Eski veri okunabiliyor — mevcut haftanın running baseline'ı olarak taşı.
            Timber.i(
                "PulseHistoryPrefs: legacy pulse_week_score=%d migrated as running baseline for week=%d",
                legacyScore,
                currentWeekStartEpochDay,
            )
            editor.putLong(KEY_RUNNING_WEEK, currentWeekStartEpochDay)
            editor.putInt(KEY_RUNNING_SCORE, legacyScore)
        } else {
            Timber.i("PulseHistoryPrefs: no readable legacy pulse score found — starting clean")
        }
        editor.putBoolean(KEY_MIGRATED, true)
        editor.apply()
    }

    /** P0.4 istatistik sıfırlama sihirbazı ile uyumlu — trend geçmişini tamamen temizler. */
    fun clearAll(context: Context) {
        val p = prefs(context)
        val editor = p.edit()
        editor.remove(KEY_RUNNING_WEEK)
        editor.remove(KEY_RUNNING_SCORE)
        editor.remove(KEY_MIGRATED)
        closedWeekIndex(p).forEach { editor.remove(closedWeekKey(it)) }
        editor.remove(KEY_CLOSED_WEEKS_INDEX)
        editor.apply()
    }
}

/**
 * [PulseHistoryPrefs.updateCurrentWeekScore] sonucu — [previousScore] null ise UI "Veri birikiyor"
 * göstermelidir (sahte +0 karşılaştırması yasak, roadmap D01).
 */
data class PulseTrendResult(
    val previousScore: Int?,
    val scoreDelta: Int?,
)
