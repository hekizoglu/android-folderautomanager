package com.armutlu.apporganizer.domain.usecase.insight

import com.armutlu.apporganizer.domain.home.SettingsSection
import com.armutlu.apporganizer.domain.home.SmartTickerItem
import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.home.TickerAction

/**
 * Döngü G8 — Cihaz Düzeni İçgörüleri
 * (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G8, satır 57-66).
 *
 * Karar gerekçesi (plan G8): "Telefonunuz yavaş çalışıyor" tarzı teşhis YAPILMAZ — kanıtlanamaz,
 * sahte "cleaner app" imajı yaratır ve Play aldatıcı davranış politikası inceleme reddi riski
 * taşır. Bunun yerine bu üretici SADECE sayı veren, kanıtlanabilir, tek dokunuşla eyleme bağlanan
 * düzen fırsatları üretir:
 * - DEPOLAMA: cihaz doluluk yüzdesi + 90+ gündür açılmayan uygulamaların kapladığı toplam alan.
 * - KULLANILMAYAN: 90+ gündür açılmayan uygulama sayısı.
 * - BİLDİRİM YÜKÜ: haftalık toplam bildirim + en gürültücü 3 uygulamanın payı.
 * - ÖZ-TANI: kullanım izni kapalıysa (uygulamanın KENDİ ölçüm yeteneği bozuluyor — başka
 *   uygulama/sistem hakkında yargı YOK).
 *
 * Dil kuralları (plan G8, MUTLAK):
 * - Korku dili YASAK ("yavaş", "tehlike", "acil" gibi kelimeler kullanılmaz).
 * - Her içgörü en az bir SAYI içerir (kanıtlanabilir iddia).
 * - Davet dili ("incele", "gözden geçir") — emir/komut ("temizle!", "sil!") YOK.
 * - Sessiz uninstall YOK — üretilen action her zaman bir EKRANA götürür (rapor/liste), asla
 *   doğrudan kaldırma tetiklemez; gerçek kaldırma sistem uninstall dialoguyla ayrı ekranda olur.
 *
 * Saf Kotlin — Android bağımlılığı yok, unit test edilebilir. [DeviceTidinessInsights] çağıran
 * ([com.armutlu.apporganizer.domain.home.RealSmartTickerSource]) yerelleştirilmiş başlık
 * metinlerini `context.getString(...)` ile ÜRETİCİYE PARAMETRE olarak verir — bu obje kendi
 * başına hiçbir hardcoded kullanıcı-dili string TAŞIMAZ (EN/TR ikisi de çağıran tarafta çözülür).
 */
object DeviceTidinessInsights {

    /** Depolama fırsatı üretimi için minimum doluluk yüzdesi (spam önleme, plan G8). */
    const val STORAGE_FULL_THRESHOLD_PERCENT = 85

    /** Depolama fırsatı üretimi için minimum kazanç (bayt) — 500 MB (spam önleme, plan G8). */
    const val STORAGE_MIN_RECLAIMABLE_BYTES = 500L * 1024 * 1024

    /** Bir uygulamanın "uzun süredir açılmamış" sayılması için gün eşiği. */
    const val UNUSED_THRESHOLD_DAYS = 90

    /** Kullanılmayan uygulama içgörüsü üretimi için minimum uygulama sayısı. */
    const val UNUSED_MIN_APP_COUNT = 5

    /** Bildirim yükü içgörüsü üretimi için minimum haftalık toplam bildirim sayısı. */
    const val NOTIFICATION_MIN_WEEKLY_TOTAL = 200

    /** Bildirim yükü içgörüsü üretimi için en gürültücü 3 uygulamanın minimum payı (%). */
    const val NOTIFICATION_TOP3_MIN_SHARE_PERCENT = 60

    private const val MS_PER_DAY = 24L * 3600 * 1000
    private const val ITEM_EXPIRY_MS = MS_PER_DAY

    /** Tek bir uygulamanın depolama/kullanılmama hesaplaması için gereken minimum alanlar. */
    data class AppUsageSnapshot(
        val packageName: String,
        val lastUsedTimestamp: Long,
        val appSizeBytes: Long,
    )

    /** Çağıranın (context sahibi) ürettiği, zaten yerelleştirilmiş metinler. */
    data class TidinessTexts(
        val storageTitle: (percent: Int, unusedCount: Int, gbFreed: String) -> String,
        val unusedTitle: (count: Int) -> String,
        val notificationTitle: (total: Int, topSharePercent: Int) -> String,
        val diagnosticsTitle: () -> String,
        val actionInspect: String,
        val actionReport: String,
        val actionFix: String,
    )

    /**
     * DEPOLAMA fırsatı. Yalnız doluluk >= [STORAGE_FULL_THRESHOLD_PERCENT] VE 90+ gündür
     * açılmayan uygulamaların toplam boyutu >= [STORAGE_MIN_RECLAIMABLE_BYTES] ise üretilir
     * (spam önleme, plan G8). Usage-access izni yoksa (kullanılmama tespiti güvenilmez) ÜRETMEZ.
     */
    fun storageOpportunity(
        hasUsageAccessPermission: Boolean,
        totalBytes: Long,
        freeBytes: Long,
        apps: List<AppUsageSnapshot>,
        nowMillis: Long,
        texts: TidinessTexts,
    ): SmartTickerItem? {
        if (!hasUsageAccessPermission) return null
        if (totalBytes <= 0L) return null

        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPercent = ((usedBytes * 100) / totalBytes).toInt()
        if (usedPercent < STORAGE_FULL_THRESHOLD_PERCENT) return null

        val unusedApps = longUnusedApps(apps, nowMillis)
        val reclaimableBytes = unusedApps.sumOf { it.appSizeBytes }
        if (reclaimableBytes < STORAGE_MIN_RECLAIMABLE_BYTES) return null

        val gbFreed = formatGb(reclaimableBytes)
        return SmartTickerItem(
            id = "tidiness_storage_${nowMillis / ITEM_EXPIRY_MS}",
            type = SmartTickerType.CONTEXTUAL_SUGGESTION,
            title = texts.storageTitle(usedPercent, unusedApps.size, gbFreed),
            subtitle = texts.actionInspect,
            icon = "📦",
            priority = 40,
            createdAt = nowMillis,
            expiresAt = nowMillis + ITEM_EXPIRY_MS,
            action = TickerAction.OpenAppList,
            suggestionKey = "tidiness_storage",
        )
    }

    /**
     * KULLANILMAYAN uygulama fırsatı. 90+ gündür açılmayan uygulama sayısı
     * >= [UNUSED_MIN_APP_COUNT] ise üretilir. Hedef: uygulama listesine gider (EX01 pattern) —
     * sessiz kaldırma YOK, gerçek kaldırma kullanıcının kendi seçimiyle sistem dialoguyla olur.
     */
    fun unusedAppsOpportunity(
        hasUsageAccessPermission: Boolean,
        apps: List<AppUsageSnapshot>,
        nowMillis: Long,
        texts: TidinessTexts,
    ): SmartTickerItem? {
        if (!hasUsageAccessPermission) return null
        val unusedCount = longUnusedApps(apps, nowMillis).size
        if (unusedCount < UNUSED_MIN_APP_COUNT) return null

        return SmartTickerItem(
            id = "tidiness_unused_${nowMillis / ITEM_EXPIRY_MS}",
            type = SmartTickerType.CONTEXTUAL_SUGGESTION,
            title = texts.unusedTitle(unusedCount),
            subtitle = texts.actionInspect,
            icon = "🗂️",
            priority = 38,
            createdAt = nowMillis,
            expiresAt = nowMillis + ITEM_EXPIRY_MS,
            action = TickerAction.OpenAppList,
            suggestionKey = "tidiness_unused",
        )
    }

    /**
     * BİLDİRİM YÜKÜ fırsatı. Haftalık toplam >= [NOTIFICATION_MIN_WEEKLY_TOTAL] VE en gürültücü
     * 3 uygulamanın payı >= [NOTIFICATION_TOP3_MIN_SHARE_PERCENT] ise üretilir.
     * @param weeklyCountsByPackage paket -> son 7 gün bildirim sayısı (NotificationEventDao.countsSince).
     */
    fun notificationLoadOpportunity(
        weeklyCountsByPackage: List<Int>,
        nowMillis: Long,
        texts: TidinessTexts,
    ): SmartTickerItem? {
        val total = weeklyCountsByPackage.sum()
        if (total < NOTIFICATION_MIN_WEEKLY_TOTAL) return null

        val top3 = weeklyCountsByPackage.sortedDescending().take(3).sum()
        val topSharePercent = ((top3 * 100) / total)
        if (topSharePercent < NOTIFICATION_TOP3_MIN_SHARE_PERCENT) return null

        return SmartTickerItem(
            id = "tidiness_notifications_${nowMillis / ITEM_EXPIRY_MS}",
            type = SmartTickerType.CONTEXTUAL_SUGGESTION,
            title = texts.notificationTitle(total, topSharePercent),
            subtitle = texts.actionReport,
            icon = "🔔",
            priority = 42,
            createdAt = nowMillis,
            expiresAt = nowMillis + ITEM_EXPIRY_MS,
            action = TickerAction.OpenNotificationReport,
            suggestionKey = "tidiness_notifications",
        )
    }

    /**
     * ÖZ-TANI fırsatı — SADECE uygulamanın KENDİ ölçüm yeteneğinin bozulduğu durumu bildirir
     * (kullanım izni kapalı → skor/içgörüler güncellenemiyor). Başka uygulama veya sistem
     * hakkında yargı İÇERMEZ (plan G8 kısıtı).
     */
    fun selfDiagnosisOpportunity(
        hasUsageAccessPermission: Boolean,
        nowMillis: Long,
        texts: TidinessTexts,
    ): SmartTickerItem? {
        if (hasUsageAccessPermission) return null

        return SmartTickerItem(
            id = "tidiness_permission_${nowMillis / ITEM_EXPIRY_MS}",
            type = SmartTickerType.CONTEXTUAL_SUGGESTION,
            title = texts.diagnosticsTitle(),
            subtitle = texts.actionFix,
            icon = "🔧",
            priority = 44,
            createdAt = nowMillis,
            expiresAt = nowMillis + ITEM_EXPIRY_MS,
            action = TickerAction.OpenSettings(SettingsSection.STATS),
            suggestionKey = "tidiness_permission",
        )
    }

    /** Tüm G8 adaylarını tek listede döner — [RealSmartTickerSource] bu çağrıyı kullanır. */
    fun all(
        hasUsageAccessPermission: Boolean,
        totalBytes: Long,
        freeBytes: Long,
        apps: List<AppUsageSnapshot>,
        weeklyNotificationCountsByPackage: List<Int>,
        nowMillis: Long,
        texts: TidinessTexts,
    ): List<SmartTickerItem> = listOfNotNull(
        storageOpportunity(hasUsageAccessPermission, totalBytes, freeBytes, apps, nowMillis, texts),
        unusedAppsOpportunity(hasUsageAccessPermission, apps, nowMillis, texts),
        notificationLoadOpportunity(weeklyNotificationCountsByPackage, nowMillis, texts),
        selfDiagnosisOpportunity(hasUsageAccessPermission, nowMillis, texts),
    )

    private fun longUnusedApps(apps: List<AppUsageSnapshot>, nowMillis: Long): List<AppUsageSnapshot> {
        val thresholdMillis = UNUSED_THRESHOLD_DAYS.toLong() * MS_PER_DAY
        return apps.filter { app ->
            app.lastUsedTimestamp > 0L && (nowMillis - app.lastUsedTimestamp) >= thresholdMillis
        }
    }

    private fun formatGb(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f", gb)
    }
}
