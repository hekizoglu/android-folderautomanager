package com.armutlu.apporganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entity representing an undo record for a folder merge operation.
 * Stores the source category, target category, and affected app packages.
 */
@Entity(tableName = "undo_merges")
data class UndoMergeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sourceCategoryId: String,

    val targetCategoryId: String,

    // Taşınan uygulamaların package adları (comma-separated JSON list değil, pipe-delimited)
    val affectedPackages: String,

    val timestamp: Long = System.currentTimeMillis(),

    val mergedAt: Long = System.currentTimeMillis()
) : Serializable {

    /**
     * Parse affectedPackages string to List
     */
    fun getAffectedPackagesList(): List<String> {
        return if (affectedPackages.isEmpty()) {
            emptyList()
        } else {
            affectedPackages.split("|")
        }
    }

    companion object {
        /**
         * Create UndoMergeEntity from a list of packages
         */
        fun create(
            sourceCategoryId: String,
            targetCategoryId: String,
            affectedPackages: List<String>
        ): UndoMergeEntity {
            return UndoMergeEntity(
                sourceCategoryId = sourceCategoryId,
                targetCategoryId = targetCategoryId,
                affectedPackages = affectedPackages.joinToString("|")
            )
        }
    }
}
