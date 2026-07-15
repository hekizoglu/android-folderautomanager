package com.armutlu.apporganizer.utils

import android.Manifest
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.armutlu.apporganizer.BuildConfig
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.local.MissionHistoryDao
import com.armutlu.apporganizer.data.local.TaskScoreEventDao
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry
import com.armutlu.apporganizer.domain.usecase.missions.MissionEngine
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationDiagnostics
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationDiagnosticsCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal enum class WorkerKind {
    PERIODIC,
    ONE_SHOT,
}

internal enum class WorkerPlanHealth {
    NORMAL,
    NORMAL_KAPALI,
    WARNING_DISABLED_BUT_SCHEDULED,
    ERROR_ENABLED_BUT_MISSING,
}

private const val MAX_REASONABLE_SCHEDULE_HORIZON_DAYS = 3650L

internal fun workerPlanHealth(enabled: Boolean, hasWork: Boolean): WorkerPlanHealth = when {
    !enabled && !hasWork -> WorkerPlanHealth.NORMAL_KAPALI
    !enabled && hasWork -> WorkerPlanHealth.WARNING_DISABLED_BUT_SCHEDULED
    enabled && !hasWork -> WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING
    else -> WorkerPlanHealth.NORMAL
}

internal fun workerPlanHealthText(health: WorkerPlanHealth): String = when (health) {
    WorkerPlanHealth.NORMAL -> "NORMAL"
    WorkerPlanHealth.NORMAL_KAPALI -> "NORMAL_KAPALI"
    WorkerPlanHealth.WARNING_DISABLED_BUT_SCHEDULED -> "UYARI: kapali ozellik icin work mevcut"
    WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING -> "HATA: etkin fakat work bulunamadi"
}

internal fun workerTelemetryText(snapshot: WorkerTelemetryPrefs.Snapshot, formatDate: (Long) -> String): String {
    if (snapshot.lastStartedAt <= 0L &&
        snapshot.successCount == 0 &&
        snapshot.failureCount == 0
    ) {
        return "telemetry=yok"
    }
    return buildString {
        append("lastStart=${formatDate(snapshot.lastStartedAt)}")
        append(", lastSuccess=${formatDate(snapshot.lastSucceededAt)}")
        append(", lastFailure=${formatDate(snapshot.lastFailedAt)}")
        append(", durationMs=${snapshot.lastDurationMs}")
        append(", success=${snapshot.successCount}")
        append(", failure=${snapshot.failureCount}")
        append(", failureCode=${snapshot.lastFailureCode}")
    }
}

internal fun backupHealthLine(
    enabled: Boolean,
    planHealth: WorkerPlanHealth,
    lastBackupAt: Long,
    telemetry: WorkerTelemetryPrefs.Snapshot,
    formatDate: (Long) -> String,
): String {
    val preference = if (enabled) "acik" else "kapali"
    val health = when (planHealth) {
        WorkerPlanHealth.NORMAL -> "NORMAL"
        WorkerPlanHealth.NORMAL_KAPALI -> "NORMAL"
        WorkerPlanHealth.WARNING_DISABLED_BUT_SCHEDULED -> "UYARI_KAPALI_WORK_VAR"
        WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING -> "HATA_PLANLANMAMIS"
    }
    return "Auto backup: tercih=$preference, saglik=$health, sonYedek=${formatDate(lastBackupAt)}, sonHata=${telemetry.lastFailureCode}"
}

internal fun permissionHealthLine(label: String, granted: Boolean, needed: Boolean): String = when {
    !needed -> "$label: gerekli=hayir, izin=${if (granted) "granted" else "denied"}, saglik=NORMAL_KULLANILMIYOR"
    granted -> "$label: gerekli=evet, izin=granted, saglik=NORMAL"
    else -> "$label: gerekli=evet, izin=denied, saglik=KONTROL_ONERISI"
}

internal fun notificationFreshnessState(listenerEnabled: Boolean, latestAt: Long?): String = when {
    !listenerEnabled -> "listener_kapali"
    latestAt == null || latestAt <= 0L -> "KONTROL_ONERISI: listener acik fakat olay yok"
    else -> "NORMAL"
}

internal fun workerNextRunText(
    state: WorkInfo.State,
    kind: WorkerKind,
    nextScheduleTimeMillis: Long?,
    now: Long,
): String? = when (state) {
    WorkInfo.State.RUNNING -> "su anda calisiyor"
    WorkInfo.State.SUCCEEDED ->
        if (kind == WorkerKind.ONE_SHOT) "tamamlandi, sonraki calisma yok" else "tamamlandi"
    WorkInfo.State.FAILED -> "basarisiz"
    WorkInfo.State.CANCELLED -> "iptal edildi"
    WorkInfo.State.BLOCKED -> "bagimlilik bekliyor"
    WorkInfo.State.ENQUEUED -> when {
        nextScheduleTimeMillis == null -> null
        nextScheduleTimeMillis < now -> "gecmis/yeniden planlama bekliyor"
        nextScheduleTimeMillis.isReasonableScheduleTime(now) ->
            "next=${DiagnosticsReportDateFormat.format(nextScheduleTimeMillis)}"
        else -> "sonraki calisma yok"
    }
}

private fun Long.isReasonableScheduleTime(now: Long): Boolean {
    if (this == Long.MAX_VALUE) return false
    val max = now + TimeUnit.DAYS.toMillis(MAX_REASONABLE_SCHEDULE_HORIZON_DAYS)
    return this in 1..max
}

private object DiagnosticsReportDateFormat {
    private val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun format(value: Long): String = synchronized(dateTime) {
        dateTime.format(Date(value))
    }
}

@Singleton
class DiagnosticsReportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val notificationEventDao: NotificationEventDao,
    private val missionHistoryDao: MissionHistoryDao,
    private val taskScoreEventDao: TaskScoreEventDao,
) {

    suspend fun createShareIntent(): Intent {
        val file = writeReportFile()
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AppOrganizer saglik raporu")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(file.name, uri)
        }
    }

    private suspend fun writeReportFile(): File {
        val timestamp = System.currentTimeMillis()
        val file = File(
            context.cacheDir,
            "apporganizer_saglik_raporu_${FILE_TS.format(Date(timestamp))}.txt",
        )
        file.writeText(buildReport(timestamp))
        return file
    }

    private suspend fun buildReport(now: Long): String {
        val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)
        val apps = appDao.getAllApps()
        val categories = categoryDao.getAllCategories()
        val notificationTotal = notificationEventDao.totalSince(0L)
        val notificationLast7d = notificationEventDao.totalSince(now - TimeUnit.DAYS.toMillis(7))
        val notificationLast24h = notificationEventDao.totalSince(now - TimeUnit.DAYS.toMillis(1))
        val notificationLatestAt = notificationEventDao.latestPostedAt()
        val latestTaskScore = runCatching { taskScoreEventDao.getLatestEvent() }.getOrNull()
        val dailyMissionCompletions = runCatching { missionHistoryDao.getCompletionCount(MissionHistoryEntry.PERIOD_DAILY) }.getOrDefault(0)
        val weeklyMissionCompletions = runCatching { missionHistoryDao.getCompletionCount(MissionHistoryEntry.PERIOD_WEEKLY) }.getOrDefault(0)
        val viewingMissionCompletions = runCatching {
            missionHistoryDao.getCompletionCountByMissionIds(listOf(MissionEngine.DAILY_VIEW_NOTIF_REPORT))
        }.getOrDefault(0)
        val behaviorMissionCompletions = runCatching {
            missionHistoryDao.getCompletionCountByMissionIds(
                listOf(
                    MissionEngine.DAILY_SCREEN_UNDER_3H,
                    MissionEngine.DAILY_NO_LATE_NIGHT,
                    MissionEngine.DAILY_UNLOCK_UNDER_30,
                    MissionEngine.DAILY_CLASSIFICATION_CLEANUP,
                    MissionEngine.WEEKLY_SCREEN_LESS,
                    MissionEngine.WEEKLY_POSITIVE_ACTIONS,
                )
            )
        }.getOrDefault(0)
        val positiveTaskScore = runCatching { taskScoreEventDao.getPositiveScore() }.getOrDefault(0)
        val negativeTaskScore = runCatching { taskScoreEventDao.getNegativeScore() }.getOrDefault(0)
        val widgetSummary = widgetSummary()
        val workerSummary = workerSummary()
        val crashSummary = crashSummary()
        val listenerEnabled = NotificationAccessUtils.isNotificationListenerEnabled(context)
        val storageSummary = storageSummary()
        val exitSummary = exitSummary()
        val startup = StartupHealthPrefs.snapshot(context)
        val searchStats = SearchStatsPrefs.getSummary(context)
        val classificationDiagnostics = ClassificationDiagnosticsCalculator.calculate(apps, now)

        val userAppCount = apps.count { !it.isSystemApp }
        val hiddenCount = apps.count { it.isHidden }

        return renderReport(
            DiagnosticsReportSnapshot(
                generatedAt = formatDateTime(now),
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                packageVersionName = packageInfo.versionName ?: "-",
                packageLongVersionCode = packageInfo.longVersionCode,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                notificationListenerEnabled = yesNo(listenerEnabled),
                usageAccessEnabled = yesNo(UsageStatsHelper.hasPermission(context)),
                postNotificationsState = permissionState(postNotificationsGranted()),
                readContactsState = permissionState(isPermissionGranted(Manifest.permission.READ_CONTACTS)),
                coarseLocationState = permissionState(isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)),
                permissionHealthSummary = listOf(
                    permissionHealthLine("Konum", isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION), AppPrefs.isHomeWeatherEnabled(context)),
                    permissionHealthLine("Kisiler", isPermissionGranted(Manifest.permission.READ_CONTACTS), AppPrefs.isSearchSourceContactsEnabled(context)),
                ),
                storageSummary = storageSummary,
                totalApps = apps.size,
                userApps = userAppCount,
                systemApps = apps.size - userAppCount,
                hiddenApps = hiddenCount,
                categoryCount = categories.size,
                lastReconcileAt = formatDateTime(AppPrefs.getLastReconcileTime(context)),
                lastUsageSyncAt = formatDateTime(AppPrefs.getLastUsageSyncTime(context)),
                classificationMode = AppPrefs.getClassificationMode(context).name,
                classificationDiagnostics = classificationDiagnostics,
                searchSourcesLine = "apps=${yesNo(AppPrefs.isSearchSourceAppsEnabled(context))}, categories=${yesNo(AppPrefs.isSearchSourceCategoriesEnabled(context))}, settings=${yesNo(AppPrefs.isSearchSourceSettingsEnabled(context))}, contacts=${yesNo(AppPrefs.isSearchSourceContactsEnabled(context))}, files=${yesNo(AppPrefs.isSearchSourceFilesEnabled(context))}",
                fileIndexItemCount = AppPrefs.getFileIndexItemCount(context),
                fileIndexLastIndexedAt = formatDateTime(AppPrefs.getFileIndexLastIndexedAt(context)),
                fileIndexFailureReason = AppPrefs.getFileIndexFailureReason(context) ?: "-",
                searchCounterLine = SearchDiagnosticsFormatter.counterLine(searchStats),
                searchInteractionLine = SearchDiagnosticsFormatter.interactionLine(searchStats),
                searchClickSourcesLine = SearchDiagnosticsFormatter.sourceLine(searchStats),
                searchActionLine = SearchDiagnosticsFormatter.actionLine(searchStats),
                searchAvgQueryLengthLine = SearchDiagnosticsFormatter.avgQueryLengthLine(searchStats),
                notificationAnalyticsEnabled = yesNo(AppPrefs.isNotifAnalyticsEnabled(context)),
                notificationTotal = notificationTotal,
                notificationLast7d = notificationLast7d,
                notificationLast24h = notificationLast24h,
                notificationLatestAt = formatDateTime(notificationLatestAt ?: 0L),
                notificationFreshness = notificationFreshnessState(listenerEnabled, notificationLatestAt),
                exitSummary = exitSummary,
                startupSummary = "Son cold=${startup.coldMs.asMetric()}, warm=${startup.warmMs.asMetric()}, ana ekran hazir=${startup.homeReadyMs.asMetric()}",
                missionsEnabled = yesNo(AppPrefs.isMissionsEnabled(context)),
                wrappedEnabled = yesNo(AppPrefs.isWrappedEnabled(context)),
                missionPrefsMigrated = yesNo(MissionPrefs.isV2Migrated(context)),
                totalStars = MissionPrefs.getTotalStars(context),
                latestMissionEvent = latestTaskScore?.let { "${it.label} (delta=${it.delta}, at=${formatDateTime(it.createdAt)})" } ?: "-",
                dailyMissionCompletions = dailyMissionCompletions,
                weeklyMissionCompletions = weeklyMissionCompletions,
                behaviorMissionCompletions = behaviorMissionCompletions,
                viewingMissionCompletions = viewingMissionCompletions,
                positiveTaskScore = positiveTaskScore,
                negativeTaskScore = negativeTaskScore,
                widgetSummary = widgetSummary,
                workerSummary = workerSummary,
                crashSummary = crashSummary,
            ),
        )
    }

    private fun widgetSummary(): String {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = WidgetPrefs.getWidgetIds(context)
        val validCount = widgetIds.count { id -> manager.getAppWidgetInfo(id) != null }
        return "Kayitli widget id: ${widgetIds.size}, provider bulunan: $validCount"
    }

    private fun workerSummary(): List<String> = WORK_SPECS.map { spec ->
        val infos = runCatching {
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(spec.uniqueName).get()
        }.getOrDefault(emptyList())
        val enabled = spec.enabled()
        val health = workerPlanHealth(enabled = enabled, hasWork = infos.isNotEmpty())
        val stateText = if (infos.isEmpty()) {
            "yok"
        } else {
            infos.joinToString(" | ") { info ->
                buildString {
                    append(info.state.name)
                    append(", attempts=${info.runAttemptCount}")
                    workerNextRunText(
                        state = info.state,
                        kind = spec.kind,
                        nextScheduleTimeMillis = info.nextScheduleTimeMillisCompat(),
                        now = System.currentTimeMillis(),
                    )?.let { append(", $it") }
                }
            }
        }
        val telemetry = WorkerTelemetryPrefs.getSnapshot(context, spec.uniqueName)
        buildString {
            append("${spec.label}: enabled=${yesNo(enabled)}, work=$stateText, durum=${workerPlanHealthText(health)}")
            append(", ${workerTelemetryText(telemetry, ::formatDateTime)}")
            if (spec.uniqueName == "auto_backup_weekly") {
                appendLine()
                append(
                    backupHealthLine(
                        enabled = enabled,
                        planHealth = health,
                        lastBackupAt = AppPrefs.getLastBackupTime(context),
                        telemetry = telemetry,
                        formatDate = ::formatDateTime,
                    )
                )
            }
        }
    }

    private fun crashSummary(): List<String> {
        val logs = CrashReporter.getAllCrashLogs(context).take(3)
        return logs.mapIndexed { index, log ->
            val summaryLine = log.lineSequence()
                .firstOrNull { it.contains("Exception") || it.contains("Error") }
                ?.take(180)
                ?: "Belirgin exception satiri bulunamadi"
            "${index + 1}. safeMode=${yesNo(CrashReporter.isSafeModeActive(context))}, summary=$summaryLine"
        }
    }

    private fun storageSummary(): String {
        val db = context.getDatabasePath("app_organizer_db")
        val room = db.length().coerceAtLeast(0L)
        val wal = File(db.path + "-wal").length().coerceAtLeast(0L)
        val shm = File(db.path + "-shm").length().coerceAtLeast(0L)
        val cache = context.cacheDir.safeTreeSize()
        return "Room=$room B, WAL=$wal B, SHM=$shm B, cache=$cache B, toplam=${room + wal + shm + cache} B"
    }

    private fun exitSummary(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "desteklenmiyor (API < 30)"
        val manager = context.getSystemService(ActivityManager::class.java) ?: return "okunamadi"
        val exits = runCatching { manager.getHistoricalProcessExitReasons(context.packageName, 0, 0) }.getOrDefault(emptyList())
        fun count(reason: Int) = exits.count { it.reason == reason }
        return "kayit=${exits.size}, ANR=${count(ApplicationExitInfo.REASON_ANR)}, lowMemory=${count(ApplicationExitInfo.REASON_LOW_MEMORY)}, nativeCrash=${count(ApplicationExitInfo.REASON_CRASH_NATIVE)}; trace=rapora_dahil_degil"
    }

    private fun File.safeTreeSize(): Long = runCatching {
        if (isFile) length() else listFiles().orEmpty().sumOf { it.safeTreeSize() }
    }.getOrDefault(0L)

    private fun Long.asMetric(): String = if (this < 0L) "olculmedi" else "${this}ms"

    private fun postNotificationsGranted(): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun permissionState(granted: Boolean?): String = when (granted) {
        true -> "granted"
        false -> "denied"
        null -> "not_required"
    }

    private fun yesNo(value: Boolean): String = if (value) "evet" else "hayir"

    private fun formatDateTime(value: Long): String {
        if (value <= 0L) return "-"
        return DATE_TIME.format(Date(value))
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.getPackageInfoCompat(packageName: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            getPackageInfo(packageName, 0)
        }

    private fun WorkInfo.nextScheduleTimeMillisCompat(): Long? {
        return runCatching { this.nextScheduleTimeMillis }
            .getOrNull()
            ?.takeIf { it > 0L }
    }

    private data class WorkerSpec(
        val label: String,
        val uniqueName: String,
        val kind: WorkerKind,
        val enabled: () -> Boolean,
    )

    private val WORK_SPECS = listOf(
        WorkerSpec("Auto backup", "auto_backup_weekly", WorkerKind.PERIODIC) { AppPrefs.isAutoBackupEnabled(context) },
        WorkerSpec("Smart insight", "smart_insight_daily", WorkerKind.PERIODIC) { AppPrefs.isSmartNotifEnabled(context) },
        WorkerSpec("Suggestion notification", "suggestion_notification_daily", WorkerKind.PERIODIC) { AppPrefs.isSuggestionNotificationsEnabled(context) },
        WorkerSpec("Weekly digest", "weekly_digest", WorkerKind.PERIODIC) { AppPrefs.isWeeklyDigestEnabled(context) },
        WorkerSpec("Files index periodic", "files_index_periodic", WorkerKind.PERIODIC) { AppPrefs.isSearchSourceFilesEnabled(context) },
        WorkerSpec("Files index one-shot", "files_index_once", WorkerKind.ONE_SHOT) { AppPrefs.isSearchSourceFilesEnabled(context) },
    )

    private companion object {
        val DATE_TIME = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val FILE_TS = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }
}

internal data class DiagnosticsReportSnapshot(
    val generatedAt: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val packageVersionName: String,
    val packageLongVersionCode: Long,
    val deviceName: String,
    val androidVersion: String,
    val notificationListenerEnabled: String,
    val usageAccessEnabled: String,
    val postNotificationsState: String,
    val readContactsState: String,
    val coarseLocationState: String,
    val permissionHealthSummary: List<String>,
    val storageSummary: String,
    val totalApps: Int,
    val userApps: Int,
    val systemApps: Int,
    val hiddenApps: Int,
    val categoryCount: Int,
    val lastReconcileAt: String,
    val lastUsageSyncAt: String,
    val classificationMode: String,
    val classificationDiagnostics: ClassificationDiagnostics,
    val searchSourcesLine: String,
    val fileIndexItemCount: Int,
    val fileIndexLastIndexedAt: String,
    val fileIndexFailureReason: String,
    val searchCounterLine: String,
    val searchInteractionLine: String,
    val searchClickSourcesLine: String,
    val searchActionLine: String,
    val searchAvgQueryLengthLine: String,
    val notificationAnalyticsEnabled: String,
    val notificationTotal: Int,
    val notificationLast7d: Int,
    val notificationLast24h: Int,
    val notificationLatestAt: String,
    val notificationFreshness: String,
    val exitSummary: String,
    val startupSummary: String,
    val missionsEnabled: String,
    val wrappedEnabled: String,
    val missionPrefsMigrated: String,
    val totalStars: Int,
    val latestMissionEvent: String,
    val dailyMissionCompletions: Int,
    val weeklyMissionCompletions: Int,
    val behaviorMissionCompletions: Int,
    val viewingMissionCompletions: Int,
    val positiveTaskScore: Int,
    val negativeTaskScore: Int,
    val widgetSummary: String,
    val workerSummary: List<String>,
    val crashSummary: List<String>,
)

internal fun renderReport(snapshot: DiagnosticsReportSnapshot): String = buildString {
    appendLine("AppOrganizer Saglik Raporu")
    appendLine("Olusturma: ${snapshot.generatedAt}")
    appendLine()
    appendLine("[Uygulama]")
    appendLine("Surum: ${snapshot.appVersionName} (${snapshot.appVersionCode})")
    appendLine("Package versionName: ${snapshot.packageVersionName}")
    appendLine("Package longVersionCode: ${snapshot.packageLongVersionCode}")
    appendLine("Cihaz: ${snapshot.deviceName}")
    appendLine("Android: ${snapshot.androidVersion}")
    appendLine()
    appendLine("[Izinler]")
    appendLine("Notification listener: ${snapshot.notificationListenerEnabled}")
    appendLine("Usage access: ${snapshot.usageAccessEnabled}")
    appendLine("POST_NOTIFICATIONS: ${snapshot.postNotificationsState}")
    appendLine("READ_CONTACTS: ${snapshot.readContactsState}")
    appendLine("ACCESS_COARSE_LOCATION: ${snapshot.coarseLocationState}")
    snapshot.permissionHealthSummary.forEach { appendLine(it) }
    appendLine()
    appendLine("[Depolama]")
    appendLine(snapshot.storageSummary)
    appendLine()
    appendLine("[Baslangic ve Cikis Sagligi]")
    appendLine(snapshot.startupSummary)
    appendLine("ApplicationExitInfo: ${snapshot.exitSummary}")
    appendLine()
    appendLine("[Uygulama Katalogu]")
    appendLine("Toplam app: ${snapshot.totalApps}")
    appendLine("Kullanici app: ${snapshot.userApps}")
    appendLine("Sistem app: ${snapshot.systemApps}")
    appendLine("Gizli app: ${snapshot.hiddenApps}")
    appendLine("Kategori sayisi: ${snapshot.categoryCount}")
    appendLine("Son reconcile: ${snapshot.lastReconcileAt}")
    appendLine("Son usage sync: ${snapshot.lastUsageSyncAt}")
    appendLine()
    appendLine("[Siniflandirma]")
    appendLine("Mod: ${snapshot.classificationMode}")
    val classification = snapshot.classificationDiagnostics
    appendLine("Kullanici uygulamasi toplam: ${classification.totalUserApps}")
    appendLine("Gizli kullanici uygulamasi: ${classification.hiddenUserApps}")
    appendLine("Otomatik kabul edilen: ${classification.automaticAccepted}")
    appendLine("Dikkat gereken: ${classification.needsAttention}")
    appendLine("Ertelemede: ${classification.snoozed}")
    appendLine("Kullanici onayli: ${classification.confirmed}")
    appendLine("Kullanici duzeltmis: ${classification.corrected}")
    appendLine("Atlanmis: ${classification.skipped}")
    appendLine("Kategorisiz/bos: ${classification.uncategorized}")
    appendLine("Gecersiz/bilinmeyen durum: ${classification.invalidOrUnknown}")
    appendLine("Sayac toplami: ${classification.reconciledTotal}")
    appendLine("Tutarlilik: ${if (classification.isConsistent) "OK" else "MISMATCH"}")
    appendLine("Dikkat nedenleri: ${classification.attentionByReason.entries.joinToString { "${it.key.name}=${it.value}" }}")
    appendLine()
    appendLine("[Arama ve Indeks]")
    appendLine("Kaynaklar: ${snapshot.searchSourcesLine}")
    appendLine("Dosya indeksi oge sayisi: ${snapshot.fileIndexItemCount}")
    appendLine("Dosya son indeksleme: ${snapshot.fileIndexLastIndexedAt}")
    appendLine("Dosya indeks hatasi: ${snapshot.fileIndexFailureReason}")
    appendLine("Diger kaynaklar: apps/categories/settings indeksleri Room icinde tutuluyor; ayri son indeks zaman damgasi kalici degil.")
    appendLine("Arama sayaci: ${snapshot.searchCounterLine}")
    appendLine("Sonuc etkilesimi: ${snapshot.searchInteractionLine}")
    appendLine("Tiklama kaynaklari: ${snapshot.searchClickSourcesLine}")
    appendLine("Hizli aksiyonlar: ${snapshot.searchActionLine}")
    appendLine("Ortalama sorgu uzunlugu: ${snapshot.searchAvgQueryLengthLine}")
    appendLine()
    appendLine("[Bildirimler]")
    appendLine("Analiz acik: ${snapshot.notificationAnalyticsEnabled}")
    appendLine("Listener acik: ${snapshot.notificationListenerEnabled}")
    appendLine("Event sayisi toplam: ${snapshot.notificationTotal}")
    appendLine("Event sayisi son 7 gun: ${snapshot.notificationLast7d}")
    appendLine("Event sayisi son 24 saat: ${snapshot.notificationLast24h}")
    appendLine("Son event zamani: ${snapshot.notificationLatestAt}")
    appendLine("Tazelik: ${snapshot.notificationFreshness}")
    appendLine("Notif metni rapora dahil edilmez.")
    appendLine()
    appendLine("[Misyon Motoru]")
    appendLine("Missions acik: ${snapshot.missionsEnabled}")
    appendLine("Wrapped acik: ${snapshot.wrappedEnabled}")
    appendLine("MissionPrefs v2 migrate: ${snapshot.missionPrefsMigrated}")
    appendLine("Toplam yildiz: ${snapshot.totalStars}")
    appendLine("Son gorev olayi: ${snapshot.latestMissionEvent}")
    appendLine("Tamamlanan gunluk gorev: ${snapshot.dailyMissionCompletions}")
    appendLine("Tamamlanan haftalik gorev: ${snapshot.weeklyMissionCompletions}")
    appendLine("Davranis degisikligi gorevi: ${snapshot.behaviorMissionCompletions}")
    appendLine("Goruntuleme gorevi: ${snapshot.viewingMissionCompletions}")
    appendLine("Gorev skoru: pozitif=${snapshot.positiveTaskScore}, negatif=${snapshot.negativeTaskScore}, net=${snapshot.positiveTaskScore + snapshot.negativeTaskScore}")
    appendLine("Dijital yasam skoru toplam yildizdan bagimsizdir.")
    appendLine("Tekrar odul engeli: aktif (ayni rapor goruntulemesi gunde bir kez puanlanir; misyonlar donem basina tekildir)")
    appendLine()
    appendLine("[Widgetler]")
    appendLine(snapshot.widgetSummary)
    appendLine()
    appendLine("[Worker Ozeti]")
    snapshot.workerSummary.forEach { appendLine(it) }
    appendLine()
    appendLine("[Kritik Hatalar]")
    if (!classification.isConsistent) {
        appendLine("Siniflandirma sayac uyusmazligi: userApps=${classification.totalUserApps}, bucketTotal=${classification.reconciledTotal}")
    }
    if (snapshot.crashSummary.isEmpty()) {
        if (classification.isConsistent) appendLine("Crash kaydi yok.")
    } else {
        snapshot.crashSummary.forEach { appendLine(it) }
    }
    appendLine()
    appendLine("[Gizlilik Notu]")
    appendLine("Bu rapor paket listesi, bildirim metni, kisi adi/numarasi ve arama sorgulari icermez.")
}
