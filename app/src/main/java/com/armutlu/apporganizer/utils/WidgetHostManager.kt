package com.armutlu.apporganizer.utils

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context

/**
 * AppWidgetHost'u uygulama boyunca tek bir instance olarak yönetir.
 * LauncherActivity.onResume'da startListening(), onPause'da stopListening() çağrılmalı.
 */
object WidgetHostManager {
    private const val HOST_ID = 1337

    @Volatile private var host: AppWidgetHost? = null

    fun getOrCreate(context: Context): AppWidgetHost {
        return host ?: synchronized(this) {
            host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }
        }
    }

    fun startListening(context: Context) {
        getOrCreate(context).startListening()
    }

    fun stopListening() {
        host?.stopListening()
    }

    /** Yeni bir widget ID tahsis eder. Kullanılmayan ID'ler deleteId ile serbest bırakılmalı. */
    fun allocateId(context: Context): Int = getOrCreate(context).allocateAppWidgetId()

    fun deleteId(context: Context, id: Int) {
        getOrCreate(context).deleteAppWidgetId(id)
    }

    /** Widget view'ını oluşturur. Info null ise (widget kaldırılmış) null döner. */
    fun createView(context: Context, id: Int): AppWidgetHostView? {
        val manager = AppWidgetManager.getInstance(context)
        val info = manager.getAppWidgetInfo(id) ?: return null
        return getOrCreate(context).createView(context, id, info)
    }
}
