package com.armutlu.apporganizer.telemetry

/** Closed, privacy-safe analytics catalog. Raw user/app data cannot enter an event. */
sealed class TelemetryEvent(
    val eventName: String,
    internal val parameters: Map<String, String> = emptyMap()
) {
    data class OnboardingStarted(val entryType: EntryType) : TelemetryEvent("onboarding_started", p("entry_type", entryType))
    data class OnboardingStep(val step: OnboardingStepType, val result: Result) : TelemetryEvent("onboarding_step", p("step", step, "result", result))
    data class OnboardingCompleted(val duration: DurationBucket) : TelemetryEvent("onboarding_completed", p("duration_bucket", duration))
    data class PermissionResult(val permissionType: PermissionType, val result: Result) : TelemetryEvent("permission_result", p("permission_type", permissionType, "result", result))
    data class SearchPerformed(val result: ResultBucket, val latency: LatencyBucket, val sourceMix: SearchSourceMix) : TelemetryEvent("search_performed", p("result_bucket", result, "latency_bucket", latency, "source_mix", sourceMix))
    data class SearchResultOpened(val sourceType: SourceType, val position: PositionBucket) : TelemetryEvent("search_result_opened", p("source_type", sourceType, "position_bucket", position))
    data class QuickActionUsed(val actionType: ActionType, val sourceType: SourceType) : TelemetryEvent("quick_action_used", p("action_type", actionType, "source_type", sourceType))
    data class FolderOpened(val folderType: FolderType, val appCount: FolderAppCountBucket) : TelemetryEvent("folder_opened", p("folder_type", folderType, "app_count_bucket", appCount))
    data class ClassificationReviewed(val decision: Decision, val confidence: ConfidenceBucket, val sourceType: SourceType) : TelemetryEvent("classification_reviewed", p("decision", decision, "confidence_bucket", confidence, "source_type", sourceType))
    data class ClassificationCorrected(val sourceType: SourceType, val confidence: ConfidenceBucket, val targetType: TargetType) : TelemetryEvent("classification_corrected", p("source_type", sourceType, "confidence_bucket", confidence, "target_type", targetType))
    data class MissionViewed(val missionType: MissionType) : TelemetryEvent("mission_viewed", p("mission_type", missionType))
    data class MissionCompleted(val missionType: MissionType, val reward: RewardBucket) : TelemetryEvent("mission_completed", p("mission_type", missionType, "reward_bucket", reward))
    data class ReportViewed(val reportType: ReportType) : TelemetryEvent("report_viewed", p("report_type", reportType))
    data class WidgetAdded(val widgetType: WidgetType) : TelemetryEvent("widget_added", p("widget_type", widgetType))
    data class HealthWarning(val warningCode: WarningCode, val severity: Severity, val version: VersionBucket) : TelemetryEvent("health_warning", p("warning_code", warningCode, "severity", severity, "version", version))
    internal class DailyUsageSummary(parameters: Map<String, String>) : TelemetryEvent("daily_usage_summary", parameters)
    internal class DailyHealthSummary(parameters: Map<String, String>) : TelemetryEvent("daily_health_summary", parameters)

    // Döngü U02 — Gizlilik güvenli telemetri (roadmap satır 1992-2055): ana ekran görev/skor/
    // şerit sistemlerinin kullanımı. Yalnız enum/sayı taşınır; başlık/uygulama/paket/bildirim/
    // dosya/klasör adı hiçbir parametreye giremez (aşağıdaki enum'lar bunu derleme zamanında
    // garanti eder — public API String parametresi kabul etmez).
    //
    // NOT: roadmap "mission_completed" ismini önerir ama bu isim yukarıda FARKLI parametrelerle
    // (mission_type + reward_bucket, Görevler ekranı ödül akışı) zaten kullanılıyor ve
    // LocalTelemetryStore günlük özet agregasyonuna bağlı — event adı/şema değiştirmek mevcut
    // agregasyonu bozar. Bu yüzden ana ekran görev kartı için "mission_card_completed" /
    // "mission_card_failed" adları kullanılır (aynı anlam, çakışmayan şema).
    data class HomeMissionCardViewed(val missionType: HomeMissionType, val status: HomeMissionStatus) : TelemetryEvent("home_mission_card_viewed", p("mission_type", missionType, "status", status))
    data class HomeMissionCardOpened(val missionType: HomeMissionType, val status: HomeMissionStatus) : TelemetryEvent("home_mission_card_opened", p("mission_type", missionType, "status", status))
    data class MissionProgressViewed(val missionType: HomeMissionType, val progress: ProgressBucket) : TelemetryEvent("mission_progress_viewed", p("mission_type", missionType, "progress_bucket", progress))
    data class MissionCardCompleted(val missionType: HomeMissionType) : TelemetryEvent("mission_card_completed", p("mission_type", missionType))
    data class MissionCardFailed(val missionType: HomeMissionType) : TelemetryEvent("mission_card_failed", p("mission_type", missionType))
    data class HomePulseCardViewed(val scoreBucket: ScoreBucket, val confidence: ConfidenceBucket) : TelemetryEvent("home_pulse_card_viewed", p("score_bucket", scoreBucket, "confidence", confidence))
    data class HomePulseCardOpened(val scoreBucket: ScoreBucket, val confidence: ConfidenceBucket) : TelemetryEvent("home_pulse_card_opened", p("score_bucket", scoreBucket, "confidence", confidence))
    data class TickerImpression(val itemType: TickerItemType, val position: PositionBucket) : TelemetryEvent("ticker_impression", p("item_type", itemType, "position_bucket", position))
    data class TickerOpened(val itemType: TickerItemType, val position: PositionBucket) : TelemetryEvent("ticker_opened", p("item_type", itemType, "position_bucket", position))
    data class TickerDismissed(val itemType: TickerItemType) : TelemetryEvent("ticker_dismissed", p("item_type", itemType))
    data class TickerSnoozed(val itemType: TickerItemType) : TelemetryEvent("ticker_snoozed", p("item_type", itemType))
    data class TickerTypeDisabled(val itemType: TickerItemType) : TelemetryEvent("ticker_type_disabled", p("item_type", itemType))
    data class TickerManualNext(val itemType: TickerItemType) : TelemetryEvent("ticker_manual_next", p("item_type", itemType))
    data class TickerAutoAdvanced(val itemType: TickerItemType) : TelemetryEvent("ticker_auto_advanced", p("item_type", itemType))

    interface WireValue { val wireValue: String }
    enum class EntryType(override val wireValue: String) : WireValue { FIRST_LAUNCH("first_launch"), SETTINGS("settings"), RECOVERY("recovery") }
    enum class OnboardingStepType(override val wireValue: String) : WireValue { WELCOME("welcome"), PERMISSIONS("permissions"), ORGANIZATION("organization"), FINISH("finish") }
    enum class Result(override val wireValue: String) : WireValue { ACCEPTED("accepted"), DENIED("denied"), SKIPPED("skipped"), SUCCESS("success"), FAILED("failed") }
    enum class DurationBucket(override val wireValue: String) : WireValue { UNDER_30S("under_30s"), S30_119("30_119s"), M2_PLUS("2m_plus"), UNKNOWN("unknown") }
    enum class PermissionType(override val wireValue: String) : WireValue { USAGE_ACCESS("usage_access"), NOTIFICATIONS("notifications"), FILES("files"), ACCESSIBILITY("accessibility") }
    enum class ResultBucket(override val wireValue: String) : WireValue { ZERO("zero"), ONE_TO_FIVE("1_5"), SIX_TO_TWENTY("6_20"), TWENTY_ONE_PLUS("21_plus") }
    enum class LatencyBucket(override val wireValue: String) : WireValue { UNDER_100_MS("under_100_ms"), MS_100_499("100_499_ms"), MS_500_PLUS("500_plus_ms"), UNKNOWN("unknown") }
    enum class SearchSourceMix(override val wireValue: String) : WireValue { APPS_ONLY("apps_only"), FILES_ONLY("files_only"), MIXED("mixed"), OTHER("other") }
    enum class SourceType(override val wireValue: String) : WireValue { APP("app"), FILE("file"), FOLDER("folder"), ACTION("action"), SYSTEM("system"), USER("user"), OTHER("other") }
    enum class PositionBucket(override val wireValue: String) : WireValue { FIRST("first"), TWO_TO_FIVE("2_5"), SIX_PLUS("6_plus") }
    enum class ActionType(override val wireValue: String) : WireValue { OPEN("open"), INFO("info"), UNINSTALL("uninstall"), FAVORITE("favorite") }
    enum class FolderType(override val wireValue: String) : WireValue { SYSTEM("system"), AUTO("auto"), USER_CREATED("user_created") }
    enum class Decision(override val wireValue: String) : WireValue { APPROVED("approved"), CORRECTED("corrected"), SKIPPED("skipped") }
    enum class ConfidenceBucket(override val wireValue: String) : WireValue { LOW("low"), MEDIUM("medium"), HIGH("high"), UNKNOWN("unknown") }
    enum class TargetType(override val wireValue: String) : WireValue { BUILTIN("builtin"), USER_CREATED("user_created"), UNCATEGORIZED("uncategorized") }
    enum class MissionType(override val wireValue: String) : WireValue { CLEANUP("cleanup"), REVIEW("review"), ORGANIZE("organize") }
    enum class RewardBucket(override val wireValue: String) : WireValue { NONE("none"), LOW("low"), MEDIUM("medium"), HIGH("high") }
    enum class ReportType(override val wireValue: String) : WireValue { WEEKLY("weekly"), PRIVACY("privacy"), NOTIFICATIONS("notifications"), HEALTH("health") }
    enum class WidgetType(override val wireValue: String) : WireValue { SEARCH("search"), FAVORITES("favorites"), FOLDER("folder") }
    enum class WarningCode(override val wireValue: String) : WireValue { SEARCH_LATENCY_HIGH("search_latency_high"), REQUIRED_PERMISSION_MISSING("required_permission_missing"), WORKER_FAILURE("worker_failure"), DATA_STALE("data_stale") }
    enum class Severity(override val wireValue: String) : WireValue { INFO("info"), WARNING("warning"), ERROR("error") }
    enum class VersionBucket(override val wireValue: String) : WireValue { CURRENT("current"), OUTDATED("outdated"), UNKNOWN("unknown") }

    // Döngü U02 — ana ekran görev/skor/şerit telemetrisi için kapalı enum'lar.
    /** [com.armutlu.apporganizer.domain.usecase.missions.MissionAction] sınıf adına eşlenir — hangi ekran türüne yönlendirdiğini taşır, görev başlığını DEĞİL. */
    enum class HomeMissionType(override val wireValue: String) : WireValue {
        CLASSIFICATION_REVIEW("classification_review"), NOTIFICATION_REPORT("notification_report"),
        USAGE_REPORT("usage_report"), USAGE_ACCESS_SETTINGS("usage_access_settings"),
        NONE("none"), UNAVAILABLE("unavailable"),
    }
    /** [com.armutlu.apporganizer.domain.usecase.missions.MissionStatus]'un wire karşılığı. */
    enum class HomeMissionStatus(override val wireValue: String) : WireValue {
        DATA_UNAVAILABLE("data_unavailable"), NOT_STARTED("not_started"), IN_PROGRESS("in_progress"),
        SAFE("safe"), AT_RISK("at_risk"), AWAITING_SETTLEMENT("awaiting_settlement"),
        COMPLETED("completed"), FAILED("failed"),
    }
    enum class ProgressBucket(override val wireValue: String) : WireValue { ZERO("0"), LOW("1_33"), MEDIUM("34_66"), HIGH("67_99"), COMPLETE("100"), UNKNOWN("unknown") }
    enum class ScoreBucket(override val wireValue: String) : WireValue { S0_19("0_19"), S20_39("20_39"), S40_59("40_59"), S60_79("60_79"), S80_100("80_100"), UNKNOWN("unknown") }
    /** [com.armutlu.apporganizer.domain.home.SmartTickerType]'ın wire karşılığı — başlık/alt başlık DEĞİL, yalnızca tür. */
    enum class TickerItemType(override val wireValue: String) : WireValue {
        CRITICAL_HEALTH("critical_health"), ACTION_REQUIRED("action_required"), MISSION_PROGRESS("mission_progress"),
        MISSION_ACHIEVEMENT("mission_achievement"), PULSE_CHANGE("pulse_change"), CONTEXTUAL_SUGGESTION("contextual_suggestion"),
        WEEKLY_REPORT("weekly_report"), FEATURE_DISCOVERY("feature_discovery"),
    }

    companion object {
        private fun p(vararg pairs: Any): Map<String, String> = pairs.asList().chunked(2).associate { (key, value) ->
            key as String to (value as WireValue).wireValue
        }
    }
}
