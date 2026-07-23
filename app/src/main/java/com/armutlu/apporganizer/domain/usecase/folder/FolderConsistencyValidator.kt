package com.armutlu.apporganizer.domain.usecase.folder

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import timber.log.Timber

/**
 * R4.2: Merge/undo sonrasında Room, manuel override, launcher klasörleri ve search index
 * tutarlılığını doğrular. İdempotent — aynı undo birden fazla çalıştırılabilir.
 */
class FolderConsistencyValidator {

    /**
     * Merge sonrasında tutarlılık kontrolü:
     * - Taşınan paketler target kategorisinde olmalı
     * - Başarılı merge öneriyi yeniden göstermeyen state'e alır
     */
    fun validateMergeConsistency(
        movedPackageNames: List<String>,
        targetCategoryId: String,
        currentApps: List<AppInfo>,
    ): ConsistencyResult {
        val issues = mutableListOf<String>()

        movedPackageNames.forEach { pkg ->
            val app = currentApps.find { it.packageName == pkg }
            if (app == null) {
                issues.add("Taşınan paket $pkg sistemde kayıp")
            } else if (app.categoryId != targetCategoryId) {
                issues.add("Paket $pkg yanlış kategoride: ${app.categoryId} (beklenen: $targetCategoryId)")
            }
        }

        return if (issues.isEmpty()) {
            Timber.d("Merge consistency ✓ — %d paket doğru kategoride", movedPackageNames.size)
            ConsistencyResult.Success
        } else {
            Timber.w("Merge consistency ✗ — %d sorun: %s", issues.size, issues.joinToString("; "))
            ConsistencyResult.Failed(issues)
        }
    }

    /**
     * Undo sonrasında tutarlılık kontrolü:
     * - Restore edilen paketler eski kategorilerine döndü mü?
     * - Sistem klasörü boşsa görünür listeden çıkmalı (DB'den değil)
     */
    fun validateUndoConsistency(
        oldCategoryMapping: Map<String, String>,
        currentApps: List<AppInfo>,
        emptyFolderCategoryIds: Set<String> = emptySet(),
    ): ConsistencyResult {
        val issues = mutableListOf<String>()

        oldCategoryMapping.forEach { (pkg, expectedCategoryId) ->
            val app = currentApps.find { it.packageName == pkg }
            if (app == null) {
                issues.add("Restore edilen paket $pkg sistemde kayıp")
            } else if (app.categoryId != expectedCategoryId) {
                issues.add("Paket $pkg yanlış kategoriye restore: ${app.categoryId} (beklenen: $expectedCategoryId)")
            }
        }

        // Boş klasörler kontrol
        emptyFolderCategoryIds.forEach { catId ->
            val hasApps = currentApps.any { it.categoryId == catId }
            if (hasApps) {
                issues.add("Klasör $catId boş olması beklendi ama uygulama var")
            }
        }

        return if (issues.isEmpty()) {
            Timber.d("Undo consistency ✓ — %d paket restore, %d boş klasör", oldCategoryMapping.size, emptyFolderCategoryIds.size)
            ConsistencyResult.Success
        } else {
            Timber.w("Undo consistency ✗ — %d sorun: %s", issues.size, issues.joinToString("; "))
            ConsistencyResult.Failed(issues)
        }
    }

    sealed class ConsistencyResult {
        object Success : ConsistencyResult()
        data class Failed(val issues: List<String>) : ConsistencyResult()

        fun isSuccess(): Boolean = this is Success
    }
}
