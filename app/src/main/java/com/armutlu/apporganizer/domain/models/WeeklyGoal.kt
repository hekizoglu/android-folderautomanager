package com.armutlu.apporganizer.domain.models

import androidx.room.Entity

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
)
