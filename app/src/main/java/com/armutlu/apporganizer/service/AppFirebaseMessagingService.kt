package com.armutlu.apporganizer.service

import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.utils.AppPrefs
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging servisi.
 *
 * Desteklenen push mesajı türleri (data payload):
 *   type = "db_update"  → AppDatabaseService.fetchAndCache() tetikler (paket→kategori DB güncelleme)
 *   type = "config"     → Gelecekte uzak yapılandırma güncellemeleri için rezerve
 *
 * Sunucudan örnek mesaj (Firebase Admin SDK / REST):
 * {
 *   "to": "<FCM_TOKEN>",
 *   "data": { "type": "db_update" }
 * }
 */
@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var appDatabaseService: AppDatabaseService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * FCM token yenilenince çağrılır.
     * Token SharedPrefs'e kaydedilir; sunucu bu token üzerinden push gönderir.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token yenilendi: ${token.take(20)}…")
        AppPrefs.setFcmToken(applicationContext, token)
    }

    /**
     * Push mesajı alındığında çağrılır.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: return
        Timber.d("FCM mesajı alındı: type=$type")

        when (type) {
            MSG_TYPE_DB_UPDATE -> handleDbUpdate()
            else -> Timber.d("FCM: bilinmeyen mesaj tipi '$type', atlanıyor")
        }
    }

    private fun handleDbUpdate() {
        Timber.d("FCM db_update: AppDatabaseService güncelleniyor")
        serviceScope.launch {
            val result = appDatabaseService.fetchAndCache()
            Timber.d("FCM db_update tamamlandı: $result")
        }
    }

    companion object {
        const val MSG_TYPE_DB_UPDATE = "db_update"
    }
}
