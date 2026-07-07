package com.armutlu.apporganizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SmartInsightWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface InsightEntryPoint {
        fun appRepository(): AppRepository
        fun categoryDao(): CategoryDao
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val ctx = applicationContext
            if (!AppPrefs.isSmartNotifEnabled(ctx)) return@runCatching Result.success()

            val ep   = EntryPointAccessors.fromApplication(ctx, InsightEntryPoint::class.java)
            val repo = ep.appRepository()
            val catDao = ep.categoryDao()
            val apps = repo.getAllApps().filter { !it.isHidden && !it.isSystemApp }
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000

            val candidates = buildList<Pair<String, String>> {
                // Kullanım süresi — bugün toplam ekran süresi yoksa usageCount ile tahmin
                if (AppPrefs.isSmartNotifDailyUsage(ctx)) {
                    val topApp = apps.maxByOrNull { it.usageCount }
                    if (topApp != null && topApp.usageCount > 0L) {
                        add("Günlük Kullanım" to "📱 ${topApp.appName} bugün en çok açtığın uygulama. Ekran süren nasıl gidiyor?")
                    }
                    val sessionCount = apps.sumOf { it.usageCount }.coerceAtMost(99L)
                    if (sessionCount > 10L) {
                        add("Günlük Kullanım" to "📊 Bugün toplam $sessionCount uygulama açılışı yaptın. Verimliliğini takip et!")
                    }
                }

                // Kullanılmayan uygulamalar
                if (AppPrefs.isSmartNotifUnusedApps(ctx)) {
                    val unused3weeks = apps.filter { app ->
                        app.lastUsedTimestamp > 0L && (now - app.lastUsedTimestamp) >= 21 * dayMs
                    }
                    if (unused3weeks.isNotEmpty()) {
                        val app = unused3weeks.random()
                        add("Temizlik Önerisi" to "🗑️ ${app.appName}'ı 3 haftadır açmadın. Silmeyi düşün mü?")
                    }
                    val neverOpened = apps.filter { it.lastUsedTimestamp == 0L && (now - it.installTime) >= 14 * dayMs }
                    if (neverOpened.size >= 3) {
                        add("Temizlik Önerisi" to "📦 ${neverOpened.size} uygulama kurulduğundan beri hiç açılmamış. Bir göz at!")
                    }
                }

                // Kategori / klasör istatistikleri
                if (AppPrefs.isSmartNotifCatStats(ctx)) {
                    val cats = catDao.getAllCategoriesFlow().first()
                    val fullestCat = cats.maxByOrNull { cat ->
                        apps.count { it.categoryId == cat.categoryId }
                    }
                    if (fullestCat != null) {
                        val count = apps.count { it.categoryId == fullestCat.categoryId }
                        if (count >= 5) {
                            add("Klasör İstatistikleri" to "📁 '${fullestCat.categoryName}' klasörün en kalabalık — $count uygulama var. Alt klasöre bölmeyi dene!")
                        }
                    }
                    val uncategorized = apps.count { it.categoryId == "uncategorized" || it.categoryId.isBlank() }
                    if (uncategorized >= 5) {
                        add("Klasör İstatistikleri" to "📂 $uncategorized uygulamanın henüz bir klasörü yok. Organize etmeye ne dersin?")
                    }
                }

                // Yeni kurulanlar
                val newApps = apps.filter { (now - it.installTime) <= 3 * dayMs && it.installTime > 0L }
                if (newApps.isNotEmpty()) {
                    val app = newApps.first()
                    add("Yeni Uygulama" to "✨ ${app.appName} yeni kuruldu! Uygulamayı bir klasöre eklemek ister misin?")
                }

                // Haftalık motivasyon / soru
                val weeklyMessages = listOf(
                    "🧹 Uygulamalarını düzenlemek hafızayı özgürleştirir. Bu hafta bir klasör mü oluştursan?",
                    "💡 Biliyor muydun? Organize bir telefon, günde ortalama 20 dakika kurtarır.",
                    "🎯 Bu haftaki hedefin: Kullanmadığın 3 uygulamayı gizle veya sil.",
                    "⚡ Hızlı erişim için en çok kullandığın 5 uygulamayı en üst klasöre al.",
                    "🔋 Arka planda çalışan uygulamaları kontrol et — pilini kurtarabilirsin."
                )
                if (Random.nextInt(3) == 0) {
                    add("Haftalık İpucu" to weeklyMessages.random())
                }
            }

            if (candidates.isNotEmpty()) {
                val (title, body) = candidates.random()
                sendNotification(title, body)
            }

            Timber.d("SmartInsight: ${candidates.size} öneri oluşturuldu")
            Result.success()
        }.getOrElse { e ->
            Timber.e(e, "SmartInsight hatası")
            Result.retry()
        }
    }

    private fun sendNotification(title: String, body: String) {
        val mgr = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(mgr)

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "dashboard")
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        mgr.notify(NOTIF_ID + System.currentTimeMillis().toInt() % 100, notification)
    }

    private fun ensureChannel(mgr: NotificationManager) {
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(CHANNEL_ID, "Akıllı Bildirimler", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Günlük kullanım içgörüleri ve temizlik önerileri"
        }
        mgr.createNotificationChannel(ch)
    }

    companion object {
        private const val WORK_NAME  = "smart_insight_daily"
        private const val CHANNEL_ID = "smart_insight"
        private const val NOTIF_ID   = 2001

        fun schedule(context: Context) {
            if (!AppPrefs.isSmartNotifEnabled(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            val targetHour = AppPrefs.getSmartNotifHour(context)
            val initialDelayMs = calculateInitialDelayMs(targetHour)
            val request = PeriodicWorkRequestBuilder<SmartInsightWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        /** Şimdiki zamandan sonraki en yakın hedef-saat:00'a kalan süreyi (ms) hesaplar. */
        private fun calculateInitialDelayMs(targetHour: Int): Long {
            val calendar = java.util.Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            if (calendar.timeInMillis <= now) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis - now
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
