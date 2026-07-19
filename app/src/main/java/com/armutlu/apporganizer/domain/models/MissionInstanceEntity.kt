package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Donem boyunca sabit gorev ornegi (Dongu M01 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md).
 *
 * Amac: Gunluk/haftalik gorev listesi ve hedefleri, uygunluk/veri degisse bile donem
 * boyunca sabit kalsin. `mission_history` (tamamlanma+yildiz ledger'i) buna DOKUNULMAZ —
 * bu tablo sadece "hangi gorev, hangi donemde, hangi hedefle atandi" sabitlemesidir.
 *
 * Ayni (missionId, periodType, periodStartEpoch) kombinasyonu icin tek instance olur
 * (unique index) — insertAllIgnore ile ikinci uretim denemesi sessizce yok sayilir.
 */
@Entity(
    tableName = "mission_instances",
    indices = [
        Index(value = ["periodType", "periodStartEpoch"]),
        Index(value = ["missionId", "periodType", "periodStartEpoch"], unique = true),
    ],
)
data class MissionInstanceEntity(
    @PrimaryKey val instanceId: String,
    val missionId: String,
    val periodType: String,
    val periodStartEpoch: Long,
    val periodStartAt: Long,
    val periodEndAt: Long,
    val targetValue: Long?,
    val baselineValue: Long?,
    val starReward: Int,
    val status: String,
    val assignedAt: Long,
    val settledAt: Long?,
    val definitionVersion: Int,
) {
    companion object {
        const val PERIOD_DAILY = "daily"
        const val PERIOD_WEEKLY = "weekly"

        const val STATUS_ASSIGNED = "assigned"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"

        /**
         * F4 — 48s grace sonrasi hala veri yoksa gorev BASARISIZ degil VERI-YOK olarak kapanir:
         * odul verilmez ama basarisizlik istatistiklerini/seriyi de kirletmez (String kolon —
         * Room migration gerektirmez).
         */
        const val STATUS_DATA_UNAVAILABLE = "data_unavailable"

        const val CURRENT_DEFINITION_VERSION = 1

        /** Deterministik instanceId — ayni donem+gorev her zaman ayni id'ye map olur. */
        fun buildInstanceId(missionId: String, periodType: String, periodStartEpoch: Long): String =
            "${missionId}_${periodType}_$periodStartEpoch"
    }
}
