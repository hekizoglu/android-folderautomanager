package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import java.util.Calendar

data class InsightCard(
    val id: String,
    val type: InsightType,
    val message: String,
    val packageName: String? = null,
    val categoryId: String? = null
)

enum class InsightType {
    MORNING_HABIT,
    UNREAD_NOTIFICATIONS,
    UNUSED_APPS,
    TOP_IN_FOLDER,
    NEVER_OPENED,
    NEWLY_INSTALLED,
    CATEGORY_SUMMARY,
    WEEKLY_QUESTION
}

object InsightEngine {

    private const val PREFS_INSIGHT = "insight_rotation"
    private const val KEY_HISTORY = "last_three_ids"
    private const val KEY_LAST_REFRESH = "last_refresh_ms"
    private const val REFRESH_INTERVAL_MS = 15L * 60 * 1000 // 15 dakika

    fun shouldRefresh(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_INSIGHT, Context.MODE_PRIVATE)
        val lastRefresh = prefs.getLong(KEY_LAST_REFRESH, 0L)
        return System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL_MS
    }

    fun markRefreshed(context: Context) {
        context.getSharedPreferences(PREFS_INSIGHT, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_REFRESH, System.currentTimeMillis()).apply()
    }

    fun generate(
        context: Context,
        apps: List<AppInfo>,
        categories: List<Category>,
        badgeCounts: Map<String, Int>
    ): List<InsightCard> {
        val candidates = buildCandidates(apps, categories, badgeCounts)
        if (candidates.isEmpty()) return emptyList()

        val history = getHistory(context)
        // Daha önce gösterilmeyenler önce gelsin
        val sorted = candidates.sortedBy { if (it.id in history) 1 else 0 }
        val picked = sorted.take(2)

        saveHistory(context, picked.map { it.id })
        return picked
    }

    private fun buildCandidates(
        apps: List<AppInfo>,
        categories: List<Category>,
        badgeCounts: Map<String, Int>
    ): List<InsightCard> {
        val cards = mutableListOf<InsightCard>()
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val catNameMap = categories.associate { it.categoryId to it.categoryName }
        val userApps = apps.filter { !it.isSystemApp && !it.isHidden }

        // 1. Sabah alışkanlığı
        if (hour in 6..11) {
            val topApp = userApps.filter { it.usageCount > 3L }.maxByOrNull { it.usageCount }
            if (topApp != null) {
                cards.add(InsightCard(
                    id = "morning_${topApp.packageName}",
                    type = InsightType.MORNING_HABIT,
                    message = "Sabah genelde ${topApp.appName} açıyorsun",
                    packageName = topApp.packageName
                ))
            }
        }

        // 2. Bildirim yoğunluğu
        if (badgeCounts.isNotEmpty()) {
            val catCounts = userApps
                .filter { badgeCounts.containsKey(it.packageName) }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { badgeCounts[it.packageName] ?: 0 } }
            val topCat = catCounts.maxByOrNull { it.value }
            if (topCat != null && topCat.value >= 2) {
                val catName = catNameMap[topCat.key] ?: topCat.key
                cards.add(InsightCard(
                    id = "notif_${topCat.key}",
                    type = InsightType.UNREAD_NOTIFICATIONS,
                    message = "$catName'ta ${topCat.value} okunmamış bildirim",
                    categoryId = topCat.key
                ))
            }
        }

        // 3. Uzun süredir açılmayan uygulamalar (7 gün)
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val unusedCount = userApps.count { it.lastUsedTimestamp in 1 until sevenDaysAgo }
        if (unusedCount >= 3) {
            cards.add(InsightCard(
                id = "unused_7d",
                type = InsightType.UNUSED_APPS,
                message = "Son 7 gündür açılmayan $unusedCount uygulama var"
            ))
        }

        // 4. Klasördeki en sık kullanılan
        val topApp = userApps.filter { it.usageCount > 5L }.maxByOrNull { it.usageCount }
        if (topApp != null) {
            val catName = catNameMap[topApp.categoryId] ?: topApp.categoryId
            cards.add(InsightCard(
                id = "top_${topApp.packageName}",
                type = InsightType.TOP_IN_FOLDER,
                message = "${topApp.appName}, $catName'ta en çok açılıyor",
                packageName = topApp.packageName,
                categoryId = topApp.categoryId
            ))
        }

        // 5. Hiç açılmamış uygulamalar (30+ gün)
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        val neverApp = userApps
            .filter { it.lastUsedTimestamp == 0L || it.lastUsedTimestamp < thirtyDaysAgo }
            .filter { it.usageCount == 0L }
            .randomOrNull()
        if (neverApp != null) {
            cards.add(InsightCard(
                id = "never_${neverApp.packageName}",
                type = InsightType.NEVER_OPENED,
                message = "${neverApp.appName}'ı hiç açmadın, silmeyi düşün?",
                packageName = neverApp.packageName
            ))
        }

        // 6. Yeni yüklenen (son 7 gün)
        val sevenDaysAgoInstall = now - 7L * 24 * 60 * 60 * 1000
        val newApp = userApps
            .filter { it.installTime > sevenDaysAgoInstall }
            .randomOrNull()
        if (newApp != null) {
            cards.add(InsightCard(
                id = "new_${newApp.packageName}",
                type = InsightType.NEWLY_INSTALLED,
                message = "${newApp.appName} geçen hafta kuruldu, bir bak?",
                packageName = newApp.packageName
            ))
        }

        // 7. Kategori özeti
        val bigCat = categories
            .map { cat -> cat to userApps.count { it.categoryId == cat.categoryId } }
            .filter { (_, count) -> count >= 4 }
            .maxByOrNull { (_, count) -> count }
        if (bigCat != null) {
            val (cat, count) = bigCat
            val usedInCat = userApps.count { it.categoryId == cat.categoryId && it.usageCount > 0L }
            cards.add(InsightCard(
                id = "cat_${cat.categoryId}",
                type = InsightType.CATEGORY_SUMMARY,
                message = "${cat.categoryName} klasöründe $count uygulama — $usedInCat tanesini açtın",
                categoryId = cat.categoryId
            ))
        }

        // 8. Haftalık soru (Pazartesi veya rastgele)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.MONDAY || cards.size < 2) {
            val distinctUsed = userApps.count { it.usageCount > 0L }
            if (distinctUsed > 0) {
                cards.add(InsightCard(
                    id = "weekly_distinct",
                    type = InsightType.WEEKLY_QUESTION,
                    message = "Bu hafta $distinctUsed farklı uygulama kullandın"
                ))
            }
        }

        return cards.shuffled()
    }

    private fun getHistory(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS_INSIGHT, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    private fun saveHistory(context: Context, ids: List<String>) {
        val existing = getHistory(context).toMutableList()
        existing.addAll(ids)
        val trimmed = existing.takeLast(3).joinToString(",")
        context.getSharedPreferences(PREFS_INSIGHT, Context.MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, trimmed).apply()
    }
}
