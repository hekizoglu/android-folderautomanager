package com.armutlu.apporganizer.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BackupManager
import com.armutlu.apporganizer.utils.WorkerTelemetryPrefs
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface BackupWorkerEntryPoint {
        fun appRepository(): AppRepository
    }

    override suspend fun doWork(): Result {
        val startedAt = WorkerTelemetryPrefs.markStarted(applicationContext, WORK_NAME)
        return runCatching {
            val repo = EntryPointAccessors.fromApplication(
                applicationContext,
                BackupWorkerEntryPoint::class.java
            ).appRepository()
            val json = BackupManager.exportToJson(applicationContext, repo)
            val file = File(applicationContext.filesDir, "auto_backup.json")
            file.writeText(json)
            Timber.d("Otomatik yedekleme tamamlandi: ${file.absolutePath}")

            // Drive / SAF klasörü seçildiyse oraya da yaz
            val driveFolderUriStr = com.armutlu.apporganizer.utils.AppPrefs.getDriveFolderUri(applicationContext)
            if (driveFolderUriStr != null) {
                runCatching { copyBackupToDrive(json, android.net.Uri.parse(driveFolderUriStr)) }
                    .onFailure { Timber.w(it, "Drive yedeği başarısız — yerel yedek korundu") }
            }

            com.armutlu.apporganizer.utils.AppPrefs.setLastBackupTime(applicationContext, System.currentTimeMillis())
            WorkerTelemetryPrefs.markSucceeded(applicationContext, WORK_NAME, startedAt)
            Result.success()
        }.getOrElse { e ->
            Timber.e(e, "Otomatik yedekleme hatasi")
            WorkerTelemetryPrefs.markFailed(
                applicationContext,
                WORK_NAME,
                startedAt,
                WorkerTelemetryPrefs.FAILURE_IO_ERROR,
            )
            Result.retry()
        }
    }

    private fun copyBackupToDrive(json: String, folderUri: android.net.Uri) {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val fileName = "apporganizer_backup_$timestamp.json"
        val docUri = androidx.documentfile.provider.DocumentFile
            .fromTreeUri(applicationContext, folderUri)
            ?.createFile("application/json", fileName)
            ?.uri ?: return
        applicationContext.contentResolver.openOutputStream(docUri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
        Timber.d("Drive yedeği kaydedildi: $fileName")
    }

    companion object {
        private const val WORK_NAME = "auto_backup_weekly"

        fun schedule(context: Context) {
            if (!AppPrefs.isAutoBackupEnabled(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            // Her 7 gunde bir calisdiran periyodik gorev — kullanicinin sectigi gun/saat/dakikaya gore ilk calisma zamani
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()
            val initialDelayMs = calculateInitialDelayMs(context)
            val request = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Kullanicinin sectigi gun (1=Pzt..7=Paz) + saat + dakikaya gore bir sonraki eslesen zamana kadar olan gecikmeyi hesaplar. */
        private fun calculateInitialDelayMs(context: Context): Long {
            val prefDay = AppPrefs.getBackupDayOfWeek(context) // 1=Pzt..7=Paz
            val hour = AppPrefs.getBackupHour(context)
            val minute = AppPrefs.getBackupMinute(context)
            val targetCalendarDow = if (prefDay == 7) java.util.Calendar.SUNDAY else prefDay + 1 // Calendar: Paz=1..Cmt=7

            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, targetCalendarDow)
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            if (!target.after(now)) {
                target.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
