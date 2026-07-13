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
)

object FolderSuggestionEngine {
    private const val LARGE_FOLDER_THRESHOLD = 18
    private const val SMALL_FOLDER_THRESHOLD = 2
    private const val UNUSED_DAYS = 60
    private val mergeTargets = mapOf(
        Category.CAT_VIDEO to Category.CAT_ENTERTAINMENT,
        Category.CAT_MUSIC to Category.CAT_ENTERTAINMENT,
        Category.CAT_DATING to Category.CAT_SOCIAL,
        Category.CAT_MAPS to Category.CAT_TRAVEL,
        Category.CAT_HOUSE to Category.CAT_LIFESTYLE,
        Category.CAT_BEAUTY to Category.CAT_LIFESTYLE,
        Category.CAT_EVENTS to Category.CAT_LIFESTYLE,
        Category.CAT_COMICS to Category.CAT_BOOKS,
    )

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
                val packageNames = folderApps.map { it.packageName }.sorted()
                suggestions += FolderSuggestion(
                    id = stableId(FolderSuggestionType.MERGE_SMALL_FOLDER, categoryId, packageNames),
                    type = FolderSuggestionType.MERGE_SMALL_FOLDER,
                    title = "${categoryNames[categoryId] ?: categoryId} birlestirilebilir",
                    description = "${folderApps.size} uygulamayi ${categoryNames[target] ?: target} klasorune tasi.",
                    packageNames = packageNames,
                    targetCategoryId = target,
                    confidence = 76,
                )
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
