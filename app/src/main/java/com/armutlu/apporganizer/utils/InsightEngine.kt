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
    val categoryId: String? = null,
)

enum class InsightType {
    MORNING_HABIT,
    UNREAD_NOTIFICATIONS,
    UNUSED_APPS,
    TOP_IN_FOLDER,
    NEVER_OPENED,
    NEW_INSTALL,
    LARGE_APP,
    CATEGORY_SUMMARY,
    MOTIVATIONAL,
    LONG_UNUSED,
}

object InsightEngine {

    private val MS_7D  = 7L  * 24 * 3600 * 1000
    private val MS_30D = 30L * 24 * 3600 * 1000

    fun generate(
        context: Context,
        apps: List<AppInfo>,
        categories: List<Category>,
        badgeCounts: Map<String, Int>
    ): List<InsightCard> {
        val candidates = buildCandidates(apps, categories, badgeCounts, context)
        return candidates
    }

    private fun buildCandidates(
        apps: List<AppInfo>,
        categories: List<Category>,
        badgeCounts: Map<String, Int>,
        context: Context? = null,
    ): List<InsightCard> {
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val catNameMap = categories.associate { it.categoryId to it.categoryName }
        val userApps = apps.filter { !it.isHidden && !it.isSystemApp }

        val pool = mutableListOf<InsightCard>()

        // Sabah alışkanlığı (06–11)
        if (hour in 6..11) {
            val top = userApps.filter { it.usageCount > 3 }.maxByOrNull { it.usageCount }
            if (top != null) {
                pool += InsightCard(
                    id = "morning_${top.packageName}",
                    type = InsightType.MORNING_HABIT,
                    message = "Sabah genelde ${top.appName} açıyorsun",
                    packageName = top.packageName,
                )
            }
        }

        // En çok kullanılan
        val topUsed = userApps.filter { it.usageCount > 0 }.maxByOrNull { it.usageCount }
        if (topUsed != null) {
            pool += InsightCard(
                id = "top_used_${topUsed.packageName}",
                type = InsightType.TOP_IN_FOLDER,
                message = "Bu hafta en çok ${topUsed.appName} kullandın",
                packageName = topUsed.packageName,
                categoryId = topUsed.categoryId,
            )
        }

        // Hiç açılmamış uygulama
        val neverOpened = userApps.filter { it.usageCount == 0L }.shuffled().firstOrNull()
        if (neverOpened != null) {
            pool += InsightCard(
                id = "never_${neverOpened.packageName}",
                type = InsightType.NEVER_OPENED,
                message = "${neverOpened.appName} uygulamasını hiç açmadın, bir bak?",
                packageName = neverOpened.packageName,
            )
        }

        // Yeni kurulu (7 gün içinde)
        val newApp = userApps
            .filter { it.installTime > 0 && now - it.installTime < MS_7D }
            .maxByOrNull { it.installTime }
        if (newApp != null) {
            pool += InsightCard(
                id = "new_install_${newApp.packageName}",
                type = InsightType.NEW_INSTALL,
                message = "${newApp.appName} bu hafta yüklendi — keşfettin mi?",
                packageName = newApp.packageName,
            )
        }

        // 30 gündür açılmayan spesifik uygulama
        val longUnused = userApps
            .filter { it.usageCount > 0 && it.lastUsedTimestamp in 1 until (now - MS_30D) }
            .shuffled()
            .firstOrNull()
        if (longUnused != null) {
            pool += InsightCard(
                id = "long_unused_${longUnused.packageName}",
                type = InsightType.LONG_UNUSED,
                message = "${longUnused.appName} uygulamasını 30 gündür açmadın, silmeyi düşün?",
                packageName = longUnused.packageName,
            )
        }

        // Genel kullanılmayan sayısı
        val unusedCount = userApps.count { it.lastUsedTimestamp in 1 until (now - MS_7D) }
        if (unusedCount >= 5) {
            pool += InsightCard(
                id = "unused_count_$unusedCount",
                type = InsightType.UNUSED_APPS,
                message = "Son 7 gündür $unusedCount uygulaman açılmadı",
            )
        }

        // En büyük uygulama
        val bigApp = userApps.filter { it.appSizeBytes > 50 * 1024 * 1024L }.maxByOrNull { it.appSizeBytes }
        if (bigApp != null) {
            val sizeMb = bigApp.appSizeBytes / (1024 * 1024)
            pool += InsightCard(
                id = "large_${bigApp.packageName}",
                type = InsightType.LARGE_APP,
                message = "${bigApp.appName} ${sizeMb}MB yer kaplıyor",
                packageName = bigApp.packageName,
            )
        }

        // Bildirim — kategori
        if (badgeCounts.isNotEmpty()) {
            val catCounts = userApps
                .filter { badgeCounts.containsKey(it.packageName) }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { badgeCounts[it.packageName] ?: 0 } }
            val topCat = catCounts.maxByOrNull { it.value }
            if (topCat != null && topCat.value >= 2) {
                val name = catNameMap[topCat.key] ?: topCat.key
                pool += InsightCard(
                    id = "notif_cat_${topCat.key}",
                    type = InsightType.UNREAD_NOTIFICATIONS,
                    message = "$name klasöründe ${topCat.value} okunmamış bildirim",
                    categoryId = topCat.key,
                )
            }
            // Tek uygulama çok bildirim
            val topNotifApp = badgeCounts.maxByOrNull { it.value }
            if (topNotifApp != null && topNotifApp.value >= 5) {
                val appName = userApps.find { it.packageName == topNotifApp.key }?.appName ?: ""
                if (appName.isNotBlank()) {
                    pool += InsightCard(
                        id = "notif_app_${topNotifApp.key}",
                        type = InsightType.UNREAD_NOTIFICATIONS,
                        message = "$appName bugün ${topNotifApp.value} bildirim gönderdi",
                        packageName = topNotifApp.key,
                    )
                }
            }
        }

        // Kategori özeti
        val bigCat = categories
            .map { cat -> cat to userApps.count { it.categoryId == cat.categoryId } }
            .filter { (_, cnt) -> cnt >= 5 }
            .maxByOrNull { (_, cnt) -> cnt }
        if (bigCat != null) {
            val (cat, cnt) = bigCat
            val usedInCat = userApps.count { it.categoryId == cat.categoryId && it.usageCount > 0 }
            pool += InsightCard(
                id = "cat_summary_${cat.categoryId}",
                type = InsightType.CATEGORY_SUMMARY,
                message = "${cat.categoryName} klasöründe $cnt uygulamadan $usedInCat'ini açtın",
                categoryId = cat.categoryId,
            )
        }

        // Motivasyon
        val distinctUsed = userApps.count { it.usageCount > 0 }
        if (distinctUsed > 0) {
            pool += InsightCard(
                id = "motivational_$distinctUsed",
                type = InsightType.MOTIVATIONAL,
                message = "Bu hafta $distinctUsed farklı uygulama kullandın",
            )
        }

        // Geçmiş kontrolü → yeni kartları önce göster
        val history = context?.let { InsightPrefs.getHistory(it) } ?: emptySet()
        val fresh = pool.filter { it.id !in history }.shuffled()
        val stale = pool.filter { it.id in history }.shuffled()
        val selected = (fresh + stale).take(2)

        if (context != null && selected.isNotEmpty()) {
            InsightPrefs.addToHistory(context, selected.map { it.id })
        }

        return selected
    }
}
