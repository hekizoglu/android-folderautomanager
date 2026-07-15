package com.armutlu.apporganizer.domain.usecase.pulse

import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dijital Nabız skor motoru V2 — saf Kotlin, tek skor kaynağı.
 * Ana ekran (Pulse Clock), Rapor Merkezi ve Haftalık Rapor AYNI motoru kullanır.
 *
 * Tasarım ilkeleri:
 * - Yüksek sosyal medya/oyun kullanımı TEK BAŞINA ceza DEĞİLDİR (V1'deki -15 kaldırıldı).
 * - Eksik veri asla 0 puan üretmez: ilgili alt skor NÖTR kalır, confidence düşer.
 * - Kullanıcı kendi geçmişine göre değerlendirilir (previous snapshot ile karşılaştırma).
 * - Yüksek kilit açma sayısı TEK BAŞINA ceza değildir; yalnızca sert değişkenlik etkiler.
 */
object DigitalPulseEngine {

    // Ağırlıklar: Düzen %25 · Dikkat %25 · Denge %20 · Temizlik %15 · İstikrar %15
    const val WEIGHT_ORGANIZATION = 0.25
    const val WEIGHT_ATTENTION = 0.25
    const val WEIGHT_BALANCE = 0.20
    const val WEIGHT_CLEANUP = 0.15
    const val WEIGHT_CONSISTENCY = 0.15

    /** Veri yokken alt skorun aldığı nötr değer — asla 0 verilmez. */
    const val NEUTRAL_SUBSCORE = 60

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val UNUSED_THRESHOLD_DAYS = 60
    private const val NEW_APP_GRACE_DAYS = 30
    private const val UNCATEGORIZED_WARN_COUNT = 10
    private val UNCATEGORIZED_IDS = setOf("uncategorized", "other")

    fun compute(input: PulseInput): DigitalPulseScore {
        val apps = input.apps.filter { !it.isHidden }
        val reasons = mutableListOf<PulseScoreReason>()

        val organization = computeOrganization(apps, reasons)
        val attention = computeAttention(input.notification, reasons)
        val balance = computeBalance(apps, input.previousCategoryUsage, reasons)
        val cleanup = computeCleanup(apps, input.nowMillis, reasons)
        val consistency = computeConsistency(input.unlockCount, input.previousUnlockCount, reasons)
        val taskContribution = computeTaskContribution(input.taskScoreContribution, reasons)

        val weightedTotal = (
            organization * WEIGHT_ORGANIZATION +
                attention * WEIGHT_ATTENTION +
                balance * WEIGHT_BALANCE +
                cleanup * WEIGHT_CLEANUP +
                consistency * WEIGHT_CONSISTENCY
            ).roundToInt()
        val total = (weightedTotal + taskContribution).coerceIn(0, 100)

        return DigitalPulseScore(
            total = total,
            baseScore = weightedTotal,
            taskContribution = taskContribution,
            organization = organization,
            attention = attention,
            balance = balance,
            cleanup = cleanup,
            consistency = consistency,
            confidence = computeConfidence(input, apps),
            reasons = reasons,
        )
    }

    // ── Düzen (%25): kategorilenme oranı + kategorisiz sayısı ────────────────
    // Az klasör kullanan CEZALANDIRILMAZ — yalnızca çok kategorisiz uygulama düşürür.
    private fun computeOrganization(
        apps: List<WrappedEngine.AppSnapshot>,
        reasons: MutableList<PulseScoreReason>,
    ): Int {
        if (apps.isEmpty()) return NEUTRAL_SUBSCORE
        val uncategorized = apps.count { it.categoryId in UNCATEGORIZED_IDS }
        val ratio = (apps.size - uncategorized).toFloat() / apps.size
        var score = (40 + ratio * 55).roundToInt() // 0.0→40, 1.0→95

        if (uncategorized >= UNCATEGORIZED_WARN_COUNT) {
            score -= 10
            reasons += PulseScoreReason(PulseReasonId.ORGANIZATION_UNCATEGORIZED, value = uncategorized, delta = -10)
        } else if (ratio >= 0.85f) {
            reasons += PulseScoreReason(
                PulseReasonId.ORGANIZATION_HIGH,
                value = (ratio * 100).roundToInt(),
                delta = 10,
            )
        }
        return score.coerceIn(0, 100)
    }

    // ── Dikkat yönetimi (%25): rahatsız eden/dikkat dağıtan + gece bildirimleri ──
    // Bildirim izni yoksa CEZA YOK: nötr kalır, confidence düşer.
    private fun computeAttention(
        notif: PulseNotificationSignals?,
        reasons: MutableList<PulseScoreReason>,
    ): Int {
        if (notif == null) {
            reasons += PulseScoreReason(PulseReasonId.ATTENTION_NO_PERMISSION)
            return NEUTRAL_SUBSCORE
        }
        var score = 85
        val noisyApps = notif.disturbingCount + notif.distractingCount
        if (noisyApps == 0) {
            score += 10
            reasons += PulseScoreReason(PulseReasonId.ATTENTION_CALM, delta = 10)
        } else {
            val penalty = (noisyApps * 6).coerceAtMost(30)
            score -= penalty
            reasons += PulseScoreReason(PulseReasonId.ATTENTION_NOISY, value = noisyApps, delta = -penalty)
        }
        val nightRatio = if (notif.totalNotifications > 0) {
            notif.nightCount.toFloat() / notif.totalNotifications
        } else 0f
        if (nightRatio > 0.3f) {
            score -= 10
            reasons += PulseScoreReason(
                PulseReasonId.ATTENTION_NIGHT,
                value = (nightRatio * 100).roundToInt(),
                delta = -10,
            )
        }
        return score.coerceIn(0, 100)
    }

    // ── Kullanım dengesi (%20): kendi normaline göre sert değişimler ────────
    // Bir kategorinin (ör. sosyal) yüksek olması TEK BAŞINA ceza DEĞİLDİR;
    // yalnızca önceki haftaya göre toplam pay kayması ölçülür.
    private fun computeBalance(
        apps: List<WrappedEngine.AppSnapshot>,
        previousCategoryUsage: Map<String, Long>?,
        reasons: MutableList<PulseScoreReason>,
    ): Int {
        if (previousCategoryUsage == null || previousCategoryUsage.isEmpty()) {
            reasons += PulseScoreReason(PulseReasonId.BALANCE_NO_BASELINE)
            return NEUTRAL_SUBSCORE
        }
        val currentUsage = apps.groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.usageCount } }
        val currentTotal = currentUsage.values.sum().toDouble()
        val previousTotal = previousCategoryUsage.values.sum().toDouble()
        if (currentTotal <= 0.0 || previousTotal <= 0.0) {
            reasons += PulseScoreReason(PulseReasonId.BALANCE_NO_BASELINE)
            return NEUTRAL_SUBSCORE
        }
        // Toplam pay kayması (total variation distance, 0..1): 0 = aynı dağılım, 1 = tamamen farklı.
        val allCategories = currentUsage.keys + previousCategoryUsage.keys
        val shift = allCategories.sumOf { cat ->
            val nowShare = (currentUsage[cat] ?: 0L) / currentTotal
            val prevShare = (previousCategoryUsage[cat] ?: 0L) / previousTotal
            abs(nowShare - prevShare)
        } / 2.0
        val score = (90 - shift * 60).roundToInt().coerceIn(0, 100)
        if (shift > 0.35) {
            reasons += PulseScoreReason(
                PulseReasonId.BALANCE_SHIFT,
                value = (shift * 100).roundToInt(),
                delta = -(shift * 60).roundToInt(),
            )
        } else {
            reasons += PulseScoreReason(PulseReasonId.BALANCE_STEADY, delta = 0)
        }
        return score
    }

    // ── Dijital temizlik (%15): 60+ gündür açılmayanlar ─────────────────────
    // Sistem uygulamaları ve son 30 günde kurulanlar değerlendirme DIŞI (ceza yok).
    private fun computeCleanup(
        apps: List<WrappedEngine.AppSnapshot>,
        now: Long,
        reasons: MutableList<PulseScoreReason>,
    ): Int {
        val eligible = apps.filter { app ->
            if (app.isSystemApp) return@filter false
            val installed = if (app.firstInstalledTime > 0L) app.firstInstalledTime else app.installTime
            installed > 0L && (now - installed) > NEW_APP_GRACE_DAYS * DAY_MS
        }
        if (eligible.isEmpty()) return NEUTRAL_SUBSCORE

        val unusedCount = eligible.count { app ->
            val last = if (app.lastUsedTimestamp > 0L) app.lastUsedTimestamp else app.installTime
            last > 0L && (now - last) >= UNUSED_THRESHOLD_DAYS * DAY_MS
        }
        val unusedRatio = unusedCount.toFloat() / eligible.size
        val score = (95 - unusedRatio * 70).roundToInt().coerceIn(0, 100)
        if (unusedRatio <= 0.15f) {
            reasons += PulseScoreReason(PulseReasonId.CLEANUP_TIDY, delta = 5)
        } else if (unusedCount >= 3) {
            reasons += PulseScoreReason(
                PulseReasonId.CLEANUP_UNUSED,
                value = unusedCount,
                delta = -(unusedRatio * 70).roundToInt(),
            )
        }
        return score
    }

    // ── İstikrar (%15): kilit açma trendi ───────────────────────────────────
    // Yüksek kilit açma sayısı TEK BAŞINA ceza DEĞİLDİR; yalnızca haftalar arası
    // sert değişkenlik (kendi normalinden sapma) etkiler. Veri yoksa nötr.
    private fun computeConsistency(
        unlockCount: Int?,
        previousUnlockCount: Int?,
        reasons: MutableList<PulseScoreReason>,
    ): Int {
        if (unlockCount == null || previousUnlockCount == null || previousUnlockCount <= 0) {
            reasons += PulseScoreReason(PulseReasonId.CONSISTENCY_NO_DATA)
            return NEUTRAL_SUBSCORE
        }
        val changeRatio = abs(unlockCount - previousUnlockCount).toDouble() / previousUnlockCount
        val score = (90 - changeRatio.coerceAtMost(1.0) * 40).roundToInt().coerceIn(0, 100)
        if (changeRatio > 0.5) {
            reasons += PulseScoreReason(
                PulseReasonId.CONSISTENCY_VOLATILE,
                value = (changeRatio * 100).roundToInt(),
                delta = -(changeRatio.coerceAtMost(1.0) * 40).roundToInt(),
            )
        } else {
            reasons += PulseScoreReason(PulseReasonId.CONSISTENCY_STEADY, delta = 0)
        }
        return score
    }

    // Gorev sistemi skora yalnizca sinirli bir katsayi olarak etki eder.
    // Spam'lenebilir bir toplam puan, Dijital Nabiz'i tek basina belirleyemez.
    private fun computeTaskContribution(
        taskScoreContribution: Int,
        reasons: MutableList<PulseScoreReason>,
    ): Int {
        val contribution = taskScoreContribution.coerceIn(-10, 10)
        if (contribution != 0) {
            reasons += PulseScoreReason(PulseReasonId.TASK_MISSIONS, delta = contribution)
        }
        return contribution
    }

    // ── Veri güveni ──────────────────────────────────────────────────────────
    private fun computeConfidence(input: PulseInput, apps: List<WrappedEngine.AppSnapshot>): DataConfidence {
        if (apps.isEmpty()) return DataConfidence.LOW
        var signals = 0
        if (input.notification != null) signals++
        if (input.previousCategoryUsage != null) signals++
        if (input.hasUsageAccess) signals++
        if (input.unlockCount != null && input.previousUnlockCount != null) signals++
        return when {
            signals >= 3 -> DataConfidence.HIGH
            signals == 2 -> DataConfidence.MEDIUM
            else -> DataConfidence.LOW
        }
    }
}
