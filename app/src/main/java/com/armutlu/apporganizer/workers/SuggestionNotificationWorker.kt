package com.armutlu.apporganizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Kontrol Bekleyenler (dusuk guvenli siniflandirma / klasor birlestirme onerileri) ekraninda
 * yeni oneri biriktiginde GUNDE EN FAZLA 1 ozet bildirim gonderir (ROADMAP #26).
 * Spam onlemi: son bilinen oneri sayisi AppPrefs'te tutulur, sadece sayi ARTTIYSA bildirim atilir.
 * Varsayilan: KAPALI (Yeni Ozellik = Ayarlar Kurali).
 */
class SuggestionNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface SuggestionEntryPoint {
        fun appRepository(): AppRepository
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val ctx = applicationContext
            if (!AppPrefs.isSuggestionNotificationsEnabled(ctx)) return@runCatching Result.success()
            if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                Timber.i("SuggestionNotification skipped: app notifications disabled")
                return@runCatching Result.success()
            }

            val entryPoint = EntryPointAccessors.fromApplication(ctx, SuggestionEntryPoint::class.java)
            val repo = entryPoint.appRepository()
            val pendingCount = repo.getPendingClassificationApps().first().size

            val lastCount = AppPrefs.getSuggestionNotifLastCount(ctx)
            if (pendingCount > 0 && pendingCount > lastCount) {
                sendNotification(pendingCount)
            }
            AppPrefs.setSuggestionNotifLastCount(ctx, pendingCount)

            Result.success()
        }.getOrElse { e ->
            Timber.e(e, "SuggestionNotification hatasi")
            Result.retry()
        }
    }

    private fun sendNotification(count: Int) {
        val ctx = applicationContext
        val manager = ctx.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val body = if (count == 1) {
            "1 yeni oneri var, incelemek icin dokun."
        } else {
            "$count yeni oneri var, incelemek icin dokun."
        }
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.CLASSIFICATION_REVIEW)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            NOTIF_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Duzen Onerileri")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(NOTIF_ID, notification) }
            .onFailure { Timber.w(it, "SuggestionNotification could not be posted") }
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Duzen Onerileri",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Klasor birlestirme / siniflandirma onerileri ozet bildirimi"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val WORK_NAME = "suggestion_notification_daily"
        private const val CHANNEL_ID = "suggestion_notifications"
        private const val NOTIF_ID = 3001

        fun schedule(context: Context) {
            if (!AppPrefs.isSuggestionNotificationsEnabled(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<SuggestionNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
