package com.armutlu.apporganizer.utils

import android.content.Context
import android.os.SystemClock

object StartupHealthPrefs {
    private const val PREFS = "startup_health"
    private const val COLD_MS = "last_cold_ms"
    private const val WARM_MS = "last_warm_ms"
    private const val HOME_READY_MS = "last_home_ready_ms"

    data class Snapshot(val coldMs: Long, val warmMs: Long, val homeReadyMs: Long)

    fun markReady(context: Context, startedAtElapsed: Long, cold: Boolean, home: Boolean) {
        val duration = (SystemClock.elapsedRealtime() - startedAtElapsed).coerceAtLeast(0L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(if (cold) COLD_MS else WARM_MS, duration)
            .apply { if (home) putLong(HOME_READY_MS, duration) }
            .apply()
    }

    fun snapshot(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Snapshot(prefs.getLong(COLD_MS, -1L), prefs.getLong(WARM_MS, -1L), prefs.getLong(HOME_READY_MS, -1L))
    }
}
