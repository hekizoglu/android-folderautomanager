package com.armutlu.apporganizer

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.FilesIndexWorkCoordinator
import com.armutlu.apporganizer.workers.BackupWorker
import com.armutlu.apporganizer.workers.CategoryDbUpdateWorker
import com.armutlu.apporganizer.workers.SmartInsightWorker
import com.armutlu.apporganizer.workers.SuggestionNotificationWorker
import com.armutlu.apporganizer.workers.TickerHistoryCleanupWorker
import com.armutlu.apporganizer.workers.WeeklyDigestWorker
import com.google.firebase.FirebaseApp
import com.armutlu.apporganizer.telemetry.TelemetryConsentManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

@HiltAndroidApp
class AppOrganizerApp : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MissionSchedulerEntryPoint {
        fun missionWorkScheduler(): com.armutlu.apporganizer.domain.usecase.missions.MissionWorkScheduler
    }

    override fun onCreate() {
        super.onCreate()
        // Güvenlik/gizlilik: release APK'da Logcat'e hiç log basılmaz (Crashlytics ayrı ele alınır).
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        com.armutlu.apporganizer.utils.CrashReporter.install(this)
        // google-services.json yoksa initializeApp null döner — Firebase çağrıları o durumda atlanır
        // (skipGoogleServices ile alınan build'ler açılışta ÇÖKMEZ; json eklenince otomatik aktifleşir)
        val firebaseApp = runCatching { FirebaseApp.initializeApp(this) }.getOrNull()
        if (firebaseApp != null) {
            runCatching {
                TelemetryConsentManager.initialize(this)
            }.onFailure { Timber.w(it, "Firebase servisleri başlatılamadı") }
        } else {
            Timber.w("Firebase devre dışı — google-services.json bulunamadı")
        }
        // Cold start optimizasyonu (D234): asagidaki isler ilk frame yolunda olmak zorunda degil —
        // WorkManager enqueue disk IO yapar, kanallar binder cagrisi.
        // Crash guvenligi icin Timber/CrashReporter/Firebase init yukarida main thread'de kaldi.
        Thread({
            runCatching {
                AppAnalytics.appStarted(this)
                if (AppPrefs.isAutoBackupEnabled(this)) {
                    BackupWorker.schedule(this)
                }
                enableGrantedContactSearchByDefault()
                WeeklyDigestWorker.schedule(this)
                SmartInsightWorker.schedule(this)
                FilesIndexWorkCoordinator.ensurePeriodicWorkScheduled(this)
                if (AppPrefs.isSuggestionNotificationsEnabled(this)) {
                    SuggestionNotificationWorker.schedule(this)
                }
                // Dongu M04 — bir sonraki donem sinirina (gece yarisi/hafta baslangici) tek
                // seferlik gorev sonuclandirma isi planlanir; worker kendini zincirleme yeniden
                // planlar (bkz. MissionSettlementWorker.doWork -> scheduler.scheduleNext()).
                runCatching {
                    EntryPointAccessors.fromApplication(this, MissionSchedulerEntryPoint::class.java)
                        .missionWorkScheduler()
                        .scheduleNext()
                }.onFailure { Timber.w(it, "MissionWorkScheduler baslatilamadi") }
                CategoryDbUpdateWorker.schedule(this)
                // Ticker arşivi ("Tüm haberler") günlük temizlik — 7 günden eski kayıtlar silinir.
                TickerHistoryCleanupWorker.schedule(this)
            }.onFailure { Timber.e(it, "Arka plan init hatasi") }
        }, "app-init-bg").start()
    }

    private fun enableGrantedContactSearchByDefault() {
        val contactsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (contactsGranted && !AppPrefs.hasSearchSourceContactsPreference(this)) {
            AppPrefs.setSearchSourceContactsEnabled(this, true)
        }
    }

    companion object {
        val processStartedAtElapsed: Long = SystemClock.elapsedRealtime()
        private var firstActivity = true

        @Synchronized
        fun consumeColdStart(): Boolean = firstActivity.also { firstActivity = false }
    }
}
