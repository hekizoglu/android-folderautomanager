package com.armutlu.apporganizer

import android.app.Application
import android.content.pm.ApplicationInfo
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.workers.BackupWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AppOrganizerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
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
    }
}
