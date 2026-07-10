package com.armutlu.apporganizer

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.workers.BackupWorker
import com.armutlu.apporganizer.workers.SmartInsightWorker
import com.armutlu.apporganizer.workers.WeeklyDigestWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AppOrganizerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        com.armutlu.apporganizer.utils.CrashReporter.install(this)
        // google-services.json yoksa initializeApp null döner — Firebase çağrıları o durumda atlanır
        // (skipGoogleServices ile alınan build'ler açılışta ÇÖKMEZ; json eklenince otomatik aktifleşir)
        val firebaseApp = runCatching { FirebaseApp.initializeApp(this) }.getOrNull()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (firebaseApp != null) {
            runCatching {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebug)
                // Firebase Analytics debug mode — Console > DebugView'de anlık event görünümü
                if (isDebug) {
                    FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
                }
            }.onFailure { Timber.w(it, "Firebase servisleri başlatılamadı") }
        } else {
            Timber.w("Firebase devre dışı — google-services.json bulunamadı")
        }
        // Cold start optimizasyonu (D234): asagidaki isler ilk frame yolunda olmak zorunda degil —
        // WorkManager enqueue disk IO yapar, kanallar binder cagrisi, FCM zaten async.
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
                createNotificationChannels()
                if (firebaseApp != null) fetchFcmToken()
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

    /** Bildirim kanallarını oluştur (Android 8+). */
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            FCM_CHANNEL_ID,
            "Güncellemeler",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Uygulama veritabanı ve yapılandırma güncellemeleri"
        }
        manager.createNotificationChannel(channel)
    }

    /** Uygulama açılışında FCM token'ı al ve kaydet. */
    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Timber.d("FCM token alındı: ${token.take(20)}…")
                AppPrefs.setFcmToken(this, token)
            }
            .addOnFailureListener { e ->
                Timber.w(e, "FCM token alınamadı")
            }
    }

    companion object {
        const val FCM_CHANNEL_ID = "app_updates"
    }
}
