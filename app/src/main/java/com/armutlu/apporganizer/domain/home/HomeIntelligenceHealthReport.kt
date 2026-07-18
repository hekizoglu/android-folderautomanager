package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes

/**
 * Döngü U03 — Sağlık raporuna eklenen üç yeni ana ekran zeka sistemi bölümü
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 2057-2112).
 *
 * SAF Kotlin (Android bağımlılığı yok) — koordinatörün/worker'ın DAVRANIŞINA dokunmaz,
 * sadece mevcut [HomeIntelligenceState] + settlement worker telemetrisi + bekleyen
 * instance sayısını OKUYUP raporlanabilir satırlara çevirir. Uygulama/kişi/dosya/bildirim
 * İÇERİĞİ taşımaz — sadece durum kodları, sayaçlar ve zaman damgaları.
 *
 * Uyarılar serbest metin DEĞİL, [HomeErrorCodes] sabitleridir (roadmap kabul kriteri).
 */
object HomeIntelligenceHealthReport {

    /** Bu süreden daha uzun süredir hiç settlement çalışmadıysa (veya work planlı değilse) STALE sayılır. */
    val SETTLEMENT_STALE_THRESHOLD_MS: Long = 26L * 60 * 60 * 1000 // 26 saat — günlük sınırdan biraz toleranslı

    data class Input(
        val homeIntelligenceState: HomeIntelligenceState,
        /** MissionSettlementWorker için [com.armutlu.apporganizer.utils.WorkerTelemetryPrefs.Snapshot] — son çalışma zamanı/sonucu. */
        val settlementLastSucceededAt: Long,
        val settlementLastFailedAt: Long,
        val settlementLastFailureCode: String,
        /** WorkManager'daki bir sonraki planlanan settlement işinin sınır zamanı (null = planlı iş bulunamadı). */
        val settlementNextScheduledAt: Long?,
        /** Dönemi bitmiş ama hâlâ "assigned" (henüz sonuçlandırılmamış) instance sayısı. */
        val pendingSettlementCount: Int,
        /** Şu anki zaman (test edilebilirlik için dışarıdan verilir). */
        val now: Long,
    )

    data class Section(
        val lines: List<String>,
        val warningCodes: Set<String>,
    )

    /** Görev sistemi + Dijital Yaşam + Akıllı Nabız bölümlerinin tamamı ve toplu uyarı kod kümesi. */
    data class Report(
        val missionSystem: Section,
        val digitalLife: Section,
        val smartPulseTicker: Section,
    ) {
        val allWarningCodes: Set<String> =
            missionSystem.warningCodes + digitalLife.warningCodes + smartPulseTicker.warningCodes
    }

    fun build(input: Input): Report = Report(
        missionSystem = buildMissionSystemSection(input),
        digitalLife = buildDigitalLifeSection(input),
        smartPulseTicker = buildTickerSection(input),
    )

    // ---- [Görev Sistemi] ----

    private fun buildMissionSystemSection(input: Input): Section {
        val missionResult = input.homeIntelligenceState.mission
        val warnings = mutableSetOf<String>()

        val active = missionResult !is HomeDataResult.Missing && missionResult !is HomeDataResult.Failed
        val summary = missionResult.let { result ->
            when (result) {
                is HomeDataResult.Ready -> result.value.summary
                is HomeDataResult.Stale -> result.value.summary
                else -> null
            }
        }

        val sourceState = sourceHealthCode(missionResult)
        if (sourceState != SourceHealth.READY) {
            warnings += HomeErrorCodes.MISSION_PROGRESS_DATA_STALE
        }

        val settlementStale = isSettlementStale(input)
        if (settlementStale) {
            warnings += HomeErrorCodes.MISSION_SETTLEMENT_STALE
        }

        val lines = listOf(
            "Aktif: ${yesNo(active)}",
            "Kaynak durumu: ${sourceState.name}",
            "Atanmış günlük: ${summary?.totalCount?.toString() ?: "-"}",
            "Tamamlanan günlük: ${summary?.completedCount?.toString() ?: "-"}",
            "Riskli: ${riskyCount(summary)}",
            "Settlement bekleyen: ${input.pendingSettlementCount}",
            "Son settlement: ${formatTimestamp(input.settlementLastSucceededAt)}",
            "Son worker durumu: ${workerOutcomeText(input)}",
            "Sonraki planlanan sınır: ${formatTimestamp(input.settlementNextScheduledAt ?: 0L)}",
        )

        return Section(lines, warnings)
    }

    private fun riskyCount(summary: HomeMissionSummary?): String {
        if (summary == null) return "-"
        return if (summary.urgent) "1" else "0"
    }

    private fun workerOutcomeText(input: Input): String = when {
        input.settlementLastFailedAt > input.settlementLastSucceededAt -> "FAILED(${input.settlementLastFailureCode})"
        input.settlementLastSucceededAt > 0L -> "SUCCEEDED"
        else -> "YOK"
    }

    private fun isSettlementStale(input: Input): Boolean {
        // Hiç çalışmadıysa ve bekleyen sonuçlanmamış instance varsa → STALE.
        if (input.settlementLastSucceededAt <= 0L) {
            return input.pendingSettlementCount > 0
        }
        val ageMs = input.now - input.settlementLastSucceededAt
        if (ageMs > SETTLEMENT_STALE_THRESHOLD_MS && input.pendingSettlementCount > 0) return true
        // Son çalışma başarısızlıkla sonuçlandıysa ve daha sonra başarı yoksa STALE.
        return input.settlementLastFailedAt > input.settlementLastSucceededAt
    }

    // ---- [Dijital Yaşam] ----

    private fun buildDigitalLifeSection(input: Input): Section {
        val pulseResult = input.homeIntelligenceState.pulse
        val warnings = mutableSetOf<String>()

        val sourceState = sourceHealthCode(pulseResult)
        if (sourceState != SourceHealth.READY) {
            warnings += HomeErrorCodes.PULSE_SNAPSHOT_STALE
        }

        val snapshot = pulseResult.let { result ->
            when (result) {
                is HomeDataResult.Ready -> result.value.snapshot
                is HomeDataResult.Stale -> result.value.snapshot
                else -> null
            }
        }

        val freshness = freshnessOf(snapshot?.computedAt, input.now)

        // Tek skor kaynağı doğrulaması: DigitalPulseSnapshot her zaman DigitalPulseEngine
        // üzerinden üretilir (bkz. DigitalPulseRepository sözleşmesi) — koordinatörden farklı
        // bir kaynak/skor değeri geldiği tespit edilemezse (burada yalnızca snapshot'ın kendisi
        // taşınıyor, ikinci bir bağımsız kaynak yok) mismatch YOKTUR. Snapshot mevcut olduğu
        // halde skor negatif/0-100 dışıysa (bariz bozulma) mismatch olarak işaretlenir.
        val scoreOutOfRange = snapshot?.score?.total?.let { it !in 0..100 } ?: false
        if (scoreOutOfRange) {
            warnings += HomeErrorCodes.PULSE_SOURCE_MISMATCH
        }

        val lines = listOf(
            "Kaynak durumu: ${sourceState.name}",
            "Skor: ${snapshot?.score?.total?.toString() ?: "-"}",
            "Confidence: ${snapshot?.score?.confidence?.name ?: "-"}",
            "Hesap zamanı: ${formatTimestamp(snapshot?.computedAt ?: 0L)}",
            "Veri tazeliği: ${freshness.name}",
            "Tek skor kaynağı: DigitalPulseEngine",
        )

        return Section(lines, warnings)
    }

    // ---- [Akıllı Nabız] (ticker) ----

    private fun buildTickerSection(input: Input): Section {
        val tickerResult = input.homeIntelligenceState.ticker
        val warnings = mutableSetOf<String>()

        val sourceState = sourceHealthCode(tickerResult)

        val items = tickerResult.let { result ->
            when (result) {
                is HomeDataResult.Ready -> result.value.items
                is HomeDataResult.Stale -> result.value.items
                else -> emptyList()
            }
        }

        // Şerit boşken görev/nabız tarafında gösterilebilecek somut aksiyon varsa
        // (riskli görev veya çok düşük skor gibi) TICKER_EMPTY_WITH_ACTIONABLE_ITEMS uyarılır —
        // ranker/suppression katmanı her şeyi elemiş olabilir, bu teşhis amaçlı bir sinyaldir.
        val missionSummary = input.homeIntelligenceState.mission.let { result ->
            when (result) {
                is HomeDataResult.Ready -> result.value.summary
                is HomeDataResult.Stale -> result.value.summary
                else -> null
            }
        }
        val hasActionableMissionItem = missionSummary?.urgent == true
        if (items.isEmpty() && hasActionableMissionItem) {
            warnings += HomeErrorCodes.TICKER_EMPTY_WITH_ACTIONABLE_ITEMS
        }

        val lines = listOf(
            "Kaynak durumu: ${sourceState.name}",
            "Aday: -",
            "Gösterilen: ${items.size}",
            "Sessize alınan tür: -",
            "Son seçim hatası: ${if (sourceState == SourceHealth.READY) "Yok" else sourceState.name}",
        )

        return Section(lines, warnings)
    }

    // ---- ortak yardımcılar ----

    private enum class SourceHealth { READY, STALE, MISSING, FAILED }

    private fun sourceHealthCode(result: HomeDataResult<*>): SourceHealth = when (result) {
        is HomeDataResult.Ready -> SourceHealth.READY
        is HomeDataResult.Stale -> SourceHealth.STALE
        is HomeDataResult.Missing -> SourceHealth.MISSING
        is HomeDataResult.Failed -> SourceHealth.FAILED
    }

    private fun freshnessOf(computedAt: Long?, now: Long): DataFreshness {
        if (computedAt == null || computedAt <= 0L) return DataFreshness.UNAVAILABLE
        val ageMs = now - computedAt
        return when {
            ageMs <= 5 * 60 * 1000L -> DataFreshness.LIVE
            ageMs <= 30 * 60 * 1000L -> DataFreshness.RECENT
            else -> DataFreshness.STALE
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "Evet" else "Hayır"

    private fun formatTimestamp(value: Long): String =
        if (value <= 0L) "-" else HealthReportDateFormat.format(value)
}

private object HealthReportDateFormat {
    private val dateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

    fun format(value: Long): String = synchronized(dateTime) {
        dateTime.format(java.util.Date(value))
    }
}
