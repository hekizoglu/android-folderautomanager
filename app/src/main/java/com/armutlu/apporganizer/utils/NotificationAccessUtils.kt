package com.armutlu.apporganizer.utils

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object NotificationAccessUtils {

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return containsPackage(flat, context.packageName)
    }

    internal fun containsPackage(flat: String, packageName: String): Boolean {
        return flat
            .split(':')
            .any { entry ->
                ComponentName.unflattenFromString(entry)?.packageName == packageName ||
                    entry.substringBefore('/').trim() == packageName
            }
    }
}
