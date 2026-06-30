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
        FirebaseApp.initializeApp(this)
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebug)
        // Firebase Analytics debug mode — Console > DebugView'de anlık event görünümü
        if (isDebug) {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
        }
        AppAnalytics.appStarted(this)
        if (AppPrefs.isAutoBackupEnabled(this)) {
            BackupWorker.schedule(this)
        }
        enableGrantedContactSearchByDefault()
        WeeklyDigestWorker.schedule(this)
        SmartInsightWorker.schedule(this)
        createNotificationChannels()
        fetchFcmToken()
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
