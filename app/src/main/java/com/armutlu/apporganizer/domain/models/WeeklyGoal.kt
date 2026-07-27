package com.armutlu.apporganizer.domain.models

import androidx.room.Entity

/** P3 — hedefin kullanıcı tarafından mı yoksa [AdaptiveCategoryTargetCalculator] tarafından mı üretildiği. */
enum class WeeklyGoalMode { MANUAL, AUTO }

/** P3 — hafta içi/hafta sonu görünür durum (roadmap §7/§8). Kalıcı alan — settlement sonrası sabitlenir. */
enum class WeeklyGoalStatus { LEARNING, ACTIVE, ON_TRACK, AT_RISK, EXCEEDED, COMPLETED, DATA_UNAVAILABLE, PAUSED }

/**
 * Kategori bazlı haftalık "en fazla kullanım" hedefi. P3 (roadmap §7) — `mode/status/pace/baseline`
 * alanları eklendi, [MIGRATION_24_25][com.armutlu.apporganizer.data.local.AppDatabase] ile
 * additive migration. [achievedAt] geriye dönük uyumluluk için TUTULUR — eski okuma kodları
 * kırılmaz, ama yeni [status] asıl kaynaktır (status=COMPLETED olduğunda achievedAt de dolu tutulur).
 */
@Entity(
    tableName = "weekly_goals",
    primaryKeys = ["categoryId", "weekStartEpochDay"],
)
data class WeeklyGoal(
    val categoryId: String,
    val targetMinutes: Int,
    val weekStartEpochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val achievedAt: Long = 0L,
    val mode: WeeklyGoalMode = WeeklyGoalMode.MANUAL,
    /** [AdaptiveCategoryTargetCalculator] hesaplamasının taban aldığı önceki hafta dakikası. */
    val baselineMinutes: Long? = null,
    /** Önceki (bir önceki ISO haftadaki) gerçek kategori kullanımı — blend formülü girdisi. */
    val previousWeekActualMinutes: Long? = null,
    val pace: String = "DENGELI",
    val status: WeeklyGoalStatus = WeeklyGoalStatus.ACTIVE,
    /** Bu hedefin (AUTO modda) üretildiği zaman damgası; MANUAL hedeflerde null kalabilir. */
    val generatedAt: Long? = null,
    /** Bu hedefin settlement'ının tamamlandığı zaman damgası — idempotency kontrolü için. */
    val settledAt: Long? = null,
    val algorithmVersion: Int = 1,
)
