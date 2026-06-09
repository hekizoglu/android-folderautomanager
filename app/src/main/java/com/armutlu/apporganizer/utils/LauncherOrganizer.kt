package com.armutlu.apporganizer.utils

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherOrganizer @Inject constructor(private val context: Context) {

    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        context.startActivity(intent)
    }
}
