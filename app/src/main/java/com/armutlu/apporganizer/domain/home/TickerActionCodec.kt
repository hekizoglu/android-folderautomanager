package com.armutlu.apporganizer.domain.home

/**
 * [TickerAction] <-> basit "wire format" string dönüşümü — ticker arşiv tablosunda
 * ([com.armutlu.apporganizer.data.local.TickerHistoryEntity.actionType]) saklanır.
 * Room entity'leri saf veri taşımalı (domain sealed interface'e bağımlı olmamalı) — bu yüzden
 * dönüşüm [TickerHistoryEntity] DIŞINDA, saf bir fonksiyon çifti olarak burada yaşar ve JVM
 * testle (round-trip) doğrulanır.
 *
 * Format: "<action_key>" veya "<action_key>:<param>". [decode] bilinmeyen/bozuk bir string
 * için güvenle [TickerAction.None] döner (mevcut satır asla crash etmez).
 */
object TickerActionCodec {

    private const val KEY_OPEN_FOLDER = "open_folder"
    private const val KEY_OPEN_APP = "open_app"
    private const val KEY_OPEN_APP_LIST = "open_app_list"
    private const val KEY_OPEN_CLASSIFICATION_REVIEW = "open_classification_review"
    private const val KEY_OPEN_NOTIFICATION_REPORT = "open_notification_report"
    private const val KEY_OPEN_DASHBOARD = "open_dashboard"
    private const val KEY_OPEN_WEEKLY_REPORT = "open_weekly_report"
    private const val KEY_OPEN_SETTINGS = "open_settings"
    private const val KEY_OPEN_SEARCH_STATS = "open_search_stats"
    private const val KEY_OPEN_REPORTS_CENTER = "open_reports_center"
    private const val KEY_OPEN_USAGE_REPORT = "open_usage_report"
    private const val KEY_OPEN_MISSIONS = "open_missions"
    private const val KEY_NONE = "none"

    fun encode(action: TickerAction): String = when (action) {
        is TickerAction.OpenFolder -> "$KEY_OPEN_FOLDER:${action.categoryId}"
        is TickerAction.OpenApp -> "$KEY_OPEN_APP:${action.packageName}"
        TickerAction.OpenAppList -> KEY_OPEN_APP_LIST
        TickerAction.OpenClassificationReview -> KEY_OPEN_CLASSIFICATION_REVIEW
        TickerAction.OpenNotificationReport -> KEY_OPEN_NOTIFICATION_REPORT
        TickerAction.OpenDashboard -> KEY_OPEN_DASHBOARD
        TickerAction.OpenWeeklyReport -> KEY_OPEN_WEEKLY_REPORT
        is TickerAction.OpenSettings -> "$KEY_OPEN_SETTINGS:${action.section.name}"
        TickerAction.OpenSearchStats -> KEY_OPEN_SEARCH_STATS
        TickerAction.OpenReportsCenter -> KEY_OPEN_REPORTS_CENTER
        TickerAction.OpenUsageReport -> KEY_OPEN_USAGE_REPORT
        TickerAction.OpenMissions -> KEY_OPEN_MISSIONS
        TickerAction.None -> KEY_NONE
    }

    fun decode(wire: String?): TickerAction {
        if (wire.isNullOrBlank()) return TickerAction.None
        val sepIndex = wire.indexOf(':')
        val key = if (sepIndex >= 0) wire.substring(0, sepIndex) else wire
        val param = if (sepIndex >= 0) wire.substring(sepIndex + 1) else null
        return when (key) {
            KEY_OPEN_FOLDER -> param?.let { TickerAction.OpenFolder(it) } ?: TickerAction.None
            KEY_OPEN_APP -> param?.let { TickerAction.OpenApp(it) } ?: TickerAction.None
            KEY_OPEN_APP_LIST -> TickerAction.OpenAppList
            KEY_OPEN_CLASSIFICATION_REVIEW -> TickerAction.OpenClassificationReview
            KEY_OPEN_NOTIFICATION_REPORT -> TickerAction.OpenNotificationReport
            KEY_OPEN_DASHBOARD -> TickerAction.OpenDashboard
            KEY_OPEN_WEEKLY_REPORT -> TickerAction.OpenWeeklyReport
            KEY_OPEN_SETTINGS -> {
                val section = param?.let { p ->
                    runCatching { SettingsSection.valueOf(p) }.getOrNull()
                } ?: SettingsSection.ROOT
                TickerAction.OpenSettings(section)
            }
            KEY_OPEN_SEARCH_STATS -> TickerAction.OpenSearchStats
            KEY_OPEN_REPORTS_CENTER -> TickerAction.OpenReportsCenter
            KEY_OPEN_USAGE_REPORT -> TickerAction.OpenUsageReport
            KEY_OPEN_MISSIONS -> TickerAction.OpenMissions
            else -> TickerAction.None
        }
    }
}
