package com.armutlu.apporganizer.domain.usecase.folder

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import java.util.Locale

enum class FolderSuggestionType {
    SPLIT_LARGE_FOLDER,
    MERGE_SMALL_FOLDER,
    CLEAN_UNUSED_APPS,
}

data class FolderSuggestion(
    val id: String,
    val type: FolderSuggestionType,
    val title: String,
    val description: String,
    val packageNames: List<String>,
    val targetCategoryId: String,
    val confidence: Int,
    // R3.1 - genisletilmis alanlar (klasor birlestirme inceleme akisi icin).
    // Eski cagri yerlerini bozmamak icin varsayilan degerler verildi.
    val sourceCategoryId: String = "",
    val reason: FolderSuggestionReason = FolderSuggestionReason.STATIC_MAPPING,
    val lockedPackageNames: List<String> = emptyList(),
    val sourceAppCount: Int = packageNames.size,
    val targetAppCount: Int = 0,
) {
    /** MERGE_SMALL_FOLDER onerileri icin tam birlestirme planini uretir. */
    fun toMergePlan(): FolderMergePlan = FolderMergePlan(
        sourceCategoryId = sourceCategoryId.ifBlank { targetCategoryId },
        targetCategoryId = targetCategoryId,
        movablePackageNames = packageNames,
        lockedPackageNames = lockedPackageNames,
        reason = reason,
        confidence = confidence,
        sourceAppCount = sourceAppCount,
        targetAppCount = targetAppCount,
    )
}

object FolderSuggestionEngine {
    private const val LARGE_FOLDER_THRESHOLD = 18
    private const val SMALL_FOLDER_THRESHOLD = FolderMergeCandidateScorer.DEFAULT_SMALL_FOLDER_THRESHOLD
    private const val UNUSED_DAYS = 60

    // R3.1 - sabit kategori eslestirme tablosu artik FolderMergeCandidateScorer icinde tutulur
    // (tek dogruluk kaynagi). Bu engine geriye donuk uyumluluk icin FolderSuggestion uretmeye
    // devam eder; asil skorlama/plan mantigi FolderMergeCandidateScorer.score()'a tasindi.
    private val mergeTargets = FolderMergeCandidateScorer.staticMergeTargets

    fun generate(
        apps: List<AppInfo>,
        categories: List<Category>,
        dismissedIds: Set<String>,
        snoozedUntilById: Map<String, Long>,
        now: Long = System.currentTimeMillis(),
    ): List<FolderSuggestion> {
        val categoryNames = categories.associate { it.categoryId to it.categoryName }
        val grouped = apps
            .filter { !it.isHidden && !it.isSystemApp && it.categoryId != Category.CAT_UNCATEGORIZED }
            .groupBy { it.categoryId }
        val suggestions = mutableListOf<FolderSuggestion>()

        grouped.forEach { (categoryId, folderApps) ->
            if (folderApps.size >= LARGE_FOLDER_THRESHOLD) {
                val leastUsed = folderApps
                    .sortedWith(compareBy<AppInfo> { it.usageCount }.thenBy { it.appName.lowercase(Locale("tr")) })
                    .take(folderApps.size / 3)
                    .map { it.packageName }
                    .sorted()
                suggestions += FolderSuggestion(
                    id = stableId(FolderSuggestionType.SPLIT_LARGE_FOLDER, categoryId, leastUsed),
                    type = FolderSuggestionType.SPLIT_LARGE_FOLDER,
                    title = "${categoryNames[categoryId] ?: categoryId} kalabalik",
                    description = "${folderApps.size} uygulama var. Az kullanilan ${leastUsed.size} uygulamayi Diger klasorune ayir.",
                    packageNames = leastUsed,
                    targetCategoryId = Category.CAT_OTHER,
                    confidence = 72,
                )
            }

            val target = mergeTargets[categoryId]
            if (target != null && folderApps.size in 1..SMALL_FOLDER_THRESHOLD && grouped.containsKey(target)) {
                val movable = folderApps.filterNot { it.isCategoryLocked }.map { it.packageName }.sorted()
                val locked = folderApps.filter { it.isCategoryLocked }.map { it.packageName }.sorted()
                if (movable.isNotEmpty()) {
                    suggestions += FolderSuggestion(
                        id = stableId(FolderSuggestionType.MERGE_SMALL_FOLDER, categoryId, movable),
                        type = FolderSuggestionType.MERGE_SMALL_FOLDER,
                        title = "${categoryNames[categoryId] ?: categoryId} birlestirilebilir",
                        description = "${movable.size} uygulamayi ${categoryNames[target] ?: target} klasorune tasi.",
                        packageNames = movable,
                        targetCategoryId = target,
                        confidence = 76,
                        sourceCategoryId = categoryId,
                        reason = FolderSuggestionReason.STATIC_MAPPING,
                        lockedPackageNames = locked,
                        sourceAppCount = folderApps.size,
                        targetAppCount = grouped[target]?.size ?: 0,
                    )
                }
            }
        }

        val unusedCutoff = now - UNUSED_DAYS * 24L * 60L * 60L * 1000L
        val unusedPackages = apps
            .filter {
                !it.isHidden &&
                    !it.isSystemApp &&
                    it.categoryId != Category.CAT_OTHER &&
                    it.lastUsedTimestamp in 1 until unusedCutoff
            }
            .sortedBy { it.lastUsedTimestamp }
            .take(20)
            .map { it.packageName }
            .sorted()
        if (unusedPackages.size >= 3) {
            suggestions += FolderSuggestion(
                id = stableId(FolderSuggestionType.CLEAN_UNUSED_APPS, "unused", unusedPackages),
                type = FolderSuggestionType.CLEAN_UNUSED_APPS,
                title = "Uzun suredir acilmayanlar",
                description = "${unusedPackages.size} uygulama 60+ gundur acilmamis. Diger klasorune al.",
                packageNames = unusedPackages,
                targetCategoryId = Category.CAT_OTHER,
                confidence = 68,
            )
        }

        return suggestions
            .filterNot { it.id in dismissedIds }
            .filterNot { (snoozedUntilById[it.id] ?: 0L) > now }
            .sortedWith(compareByDescending<FolderSuggestion> { it.confidence }.thenBy { it.title })
            .take(12)
    }

    private fun stableId(type: FolderSuggestionType, scope: String, packageNames: List<String>): String =
        Integer.toHexString("${type.name}:$scope:${packageNames.joinToString(",")}".hashCode())
}
