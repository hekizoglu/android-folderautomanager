package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operations")
data class Operation(
    @PrimaryKey val id: String,
    val type: String, // "FOLDER_MERGE", "FOLDER_SPLIT", etc.
    val timestamp: Long,
    val sourceCategoryId: String,
    val targetCategoryId: String? = null,
    val movedPackageNames: String, // JSON array string
    val oldCategoryMapping: String, // JSON map: packageName -> categoryId
    val rolledBack: Boolean = false,
    val rolledBackAt: Long? = null,
)
