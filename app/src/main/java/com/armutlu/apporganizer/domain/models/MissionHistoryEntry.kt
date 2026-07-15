package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "mission_history",
    indices = [
        Index(value = ["periodType", "periodStartEpoch"]),
        Index(value = ["missionId", "periodType", "periodStartEpoch"], unique = true),
    ],
)
data class MissionHistoryEntry(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val missionId: String,
    val periodType: String,
    val periodStartEpoch: Long,
    val completedAt: Long,
    val starReward: Int,
    val source: String = "auto",
) {
    companion object {
        const val PERIOD_DAILY = "daily"
        const val PERIOD_WEEKLY = "weekly"
        const val PERIOD_LEGACY = "legacy"
    }
}
