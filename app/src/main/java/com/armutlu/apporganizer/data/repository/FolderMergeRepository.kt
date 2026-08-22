package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.MergeDecisionStore
import com.armutlu.apporganizer.data.local.OperationDao
import com.armutlu.apporganizer.domain.models.Operation
import com.armutlu.apporganizer.domain.usecase.folder.FolderConsistencyValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

class FolderMergeRepository(
    private val appDao: AppDao,
    private val operationDao: OperationDao,
    private val decisionStore: MergeDecisionStore,
) {
    private val validator = FolderConsistencyValidator()

    suspend fun mergeFolders(
        sourceCategoryId: String,
        targetCategoryId: String,
        packageNames: List<String>,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val operationId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // Backup — undo için eski kategoriler kaydedilir
            val oldMapping = mutableMapOf<String, String>()
            packageNames.forEach { pkg ->
                val app = appDao.getAppByPackageName(pkg)
                if (app != null) {
                    oldMapping[pkg] = app.categoryId
                }
            }

            // Move — tüm paketleri hedef kategoriye taşı
            appDao.updateAppsCategory(packageNames, targetCategoryId)

            // Tutarlılık kontrolü (R4.2)
            val allApps = appDao.getAllApps()
            val consistencyCheck = validator.validateMergeConsistency(packageNames, targetCategoryId, allApps)
            if (!consistencyCheck.isSuccess()) {
                throw IllegalStateException(
                    "Merge consistency check failed: ${(consistencyCheck as? FolderConsistencyValidator.ConsistencyResult.Failed)?.issues}",
                )
            }

            // Log — geri almak için operation kaydedilir
            val operation = Operation(
                id = operationId,
                type = "FOLDER_MERGE",
                timestamp = timestamp,
                sourceCategoryId = sourceCategoryId,
                targetCategoryId = targetCategoryId,
                movedPackageNames = packageNames.joinToString(","),
                oldCategoryMapping = oldMapping.entries.joinToString(";") { "${it.key}:${it.value}" },
                rolledBack = false,
                rolledBackAt = null,
            )
            operationDao.insert(operation)

            // Başarılı merge → önerimi yeniden gösterme (R4.2)
            decisionStore.recordApprovedMerge(sourceCategoryId, targetCategoryId)

            Timber.d("Folder merge tx: $sourceCategoryId → $targetCategoryId, %d apps, op=$operationId", packageNames.size)

            Result.success(operationId)
        } catch (e: Exception) {
            Timber.e(e, "Folder merge tx failed")
            Result.failure(e)
        }
    }

    suspend fun undoFolderMerge(operationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val operation = operationDao.getById(operationId)
                ?: throw IllegalArgumentException("Operation $operationId not found")

            if (operation.rolledBack) {
                throw IllegalStateException("Operation $operationId already rolled back")
            }

            // Parse oldCategoryMapping: pkg1:cat1;pkg2:cat2;...
            val categoryMap = operation.oldCategoryMapping.split(";")
                .filter { it.isNotEmpty() }
                .associate { entry ->
                    val (pkg, cat) = entry.split(":")
                    pkg to cat
                }

            // Restore — eski kategorilere geri taşı
            categoryMap.forEach { (pkg, categoryId) ->
                appDao.updateCategoryForPackage(pkg, categoryId)
            }

            // Tutarlılık kontrolü (R4.2 — idempotent)
            val allApps = appDao.getAllApps()
            val consistencyCheck = validator.validateUndoConsistency(
                oldCategoryMapping = categoryMap,
                currentApps = allApps,
                emptyFolderCategoryIds = setOf(operation.sourceCategoryId),
            )
            if (!consistencyCheck.isSuccess()) {
                throw IllegalStateException(
                    "Undo consistency check failed: ${(consistencyCheck as? FolderConsistencyValidator.ConsistencyResult.Failed)?.issues}",
                )
            }

            // Mark — operation'ı rolled back işaretle
            operationDao.markRolledBack(operationId, System.currentTimeMillis())

            Timber.d("Folder merge undo: op=$operationId restored to %d apps", categoryMap.size)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Folder merge undo failed for $operationId")
            Result.failure(e)
        }
    }
}
