package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * NotificationListenerService izin durumunu kontrol eder — bildirim rozeti almak için gerekli.
 * Aktivite → Bildirim Erişimi (Settings > Apps & notifications > Notifications > Advanced > Notification access)
 */
object NotificationListenerPermissionHelper {
    private const val NOTIFICATION_LISTENER_SERVICE_ENABLED = "enabled_notification_listeners"

    /**
     * NotificationListenerService izni verilmiş mi kontrol et.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, "com.armutlu.apporganizer.domain.usecase.AppNotificationListenerService")
        val flatString = componentName.flattenToString()
        val enabledListeners = Settings.Secure.getString(context.contentResolver, NOTIFICATION_LISTENER_SERVICE_ENABLED) ?: ""
        return enabledListeners.contains(flatString)
    }

    /**
     * NotificationListenerService izin kartı gösterilmeli mi?
     * - İzin kapalıysa VE ("Hiçbir zaman" değilse) VE (snooze süresi geçtiyse) → göster
     * - Aksi halde → gizle
     */
    fun shouldShowNotificationBadgePermissionCard(context: Context): Boolean {
        if (isNotificationListenerEnabled(context)) return false  // İzin zaten var
        if (com.armutlu.apporganizer.utils.AppPrefs.isNotificationBadgePermDismissed(context)) return false  // Kalıcı gizli

        val snoozeUntil = com.armutlu.apporganizer.utils.AppPrefs.getNotificationBadgePermSnoozeUntil(context)
        return System.currentTimeMillis() >= snoozeUntil
    }

    /**
     * Kartı 1 hafta (7 gün) sessize al.
     */
    fun snoozeNotificationBadgePermissionCard(context: Context) {
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        val snoozeUntil = System.currentTimeMillis() + sevenDaysMs
        com.armutlu.apporganizer.utils.AppPrefs.setNotificationBadgePermSnoozeUntil(context, snoozeUntil)
    }

    /**
     * Kartı kalıcı gizle ("Hiçbir zaman" seçildi).
     */
    fun dismissNotificationBadgePermissionCard(context: Context) {
        com.armutlu.apporganizer.utils.AppPrefs.setNotificationBadgePermDismissed(context, true)
    }
}
