package com.armutlu.apporganizer.data.remote

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

/**
 * Service for periodic backup and sync to Google Drive.
 * Google Drive API entegrasyonu henüz yapılmadığı için servis hemen duruyor.
 * START_NOT_STICKY: Android sistem tarafından yeniden başlatılmaz → "hizmet hatalı" hatası önlenir.
 */
class BackupSyncService : Service() {

    override fun onCreate() {
        super.onCreate()
        Timber.d("BackupSyncService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("BackupSyncService started — Google Drive entegrasyonu henüz aktif değil")
        // Google Drive API entegre edilene kadar hemen dur; sistem yeniden başlatmasın
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("BackupSyncService destroyed")
    }
}
