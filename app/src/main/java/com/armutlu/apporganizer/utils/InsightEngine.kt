package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import java.util.Calendar

data class InsightCard(
    val type: InsightType,
    val message: String,
    val packageName: String? = null,
    val categoryId: String? = null
)

enum class InsightType { MORNING_HABIT, UNREAD_NOTIFICATIONS, UNUSED_APPS, TOP_IN_FOLDER }

object InsightEngine {

    fun generate(
        apps: List<AppInfo>,
        categories: List<Category>,
        badgeCounts: Map<String, Int>
    ): List<InsightCard> {
        val cards = mutableListOf<InsightCard>()
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val catNameMap = categories.associate { it.categoryId to it.categoryName }

        // 1. Sabah alışkanlığı (06-11 arası) — en çok kullanılan uygulama
        if (hour in 6..11) {
            val topApp = apps.filter { it.usageCount > 3 }.maxByOrNull { it.usageCount }
            if (topApp != null) {
                cards.add(InsightCard(
                    type = InsightType.MORNING_HABIT,
                    message = "Sabah genelde ${topApp.appName} açıyorsun",
                    packageName = topApp.packageName
                ))
            }
        }

        // 2. Bildirim yoğunluğu — en çok bildirimi olan kategori
        if (badgeCounts.isNotEmpty()) {
            val catNotifCounts = apps
                .filter { badgeCounts.containsKey(it.packageName) }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { badgeCounts[it.packageName] ?: 0 } }
            val topCat = catNotifCounts.maxByOrNull { it.value }
            if (topCat != null && topCat.value >= 2) {
                val catName = catNameMap[topCat.key] ?: topCat.key
                cards.add(InsightCard(
                    type = InsightType.UNREAD_NOTIFICATIONS,
                    message = "$catName'ta ${topCat.value} okunmamış bildirim",
                    categoryId = topCat.key
                ))
            }
        }

        // 3. Uzun süredir açılmayan uygulamalar
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val unusedCount = apps.count { !it.isSystemApp && it.lastUsedTimestamp in 1 until sevenDaysAgo }
        if (unusedCount >= 5) {
            cards.add(InsightCard(
                type = InsightType.UNUSED_APPS,
                message = "Son 7 gündür açılmayan $unusedCount uygulama var"
            ))
        }

        // 4. Kategorideki en sık kullanılan — swipe-up ipucu
        if (cards.size < 2) {
            val topApp = apps.filter { it.usageCount > 5 }.maxByOrNull { it.usageCount }
            if (topApp != null) {
                val catName = catNameMap[topApp.categoryId] ?: topApp.categoryId
                cards.add(InsightCard(
                    type = InsightType.TOP_IN_FOLDER,
                    message = "${topApp.appName}, $catName'ta en çok açılıyor",
                    packageName = topApp.packageName,
                    categoryId = topApp.categoryId
                ))
            }
        }

        return cards.take(2)
    }
}
