package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "task_score_events",
    indices = [Index(value = ["eventKey", "createdAt"])],
)
data class TaskScoreEventEntry(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val eventKey: String,
    val label: String,
    val delta: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
