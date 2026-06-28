package com.armutlu.apporganizer.utils

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {

    private const val CRASH_DIR = "crash_logs"
    private const val MAX_LOGS = 5
    private const val PREFS_CRASH = "crash_reporter_prefs"
    private const val KEY_CRASH_COUNT = "startup_crash_count"
    private const val KEY_SAFE_MODE = "safe_mode_active"
    private const val SAFE_MODE_THRESHOLD = 2

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(appContext, throwable)
                incrementCrashCount(appContext)
            } catch (e: Exception) {
                Timber.e(e, "CrashReporter: log kaydetme hatası")
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Timber.d("CrashReporter kuruldu")
    }

    fun checkSafeMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_CRASH_COUNT, 0)
        val alreadySafe = prefs.getBoolean(KEY_SAFE_MODE, false)
        return if (count >= SAFE_MODE_THRESHOLD || alreadySafe) {
            prefs.edit().putBoolean(KEY_SAFE_MODE, true).putInt(KEY_CRASH_COUNT, 0).apply()
            true
        } else {
            false
        }
    }

    fun markStartedSuccessfully(context: Context) {
        context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CRASH_COUNT, 0).apply()
    }

    fun exitSafeMode(context: Context) {
        context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SAFE_MODE, false).putInt(KEY_CRASH_COUNT, 0).apply()
    }

    fun isSafeModeActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
            .getBoolean(KEY_SAFE_MODE, false)

    private fun incrementCrashCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_CRASH_COUNT, count).apply()
        Timber.w("Startup crash sayacı: $count")
    }

    private fun saveCrashLog(context: Context, throwable: Throwable) {
        val dir = File(context.filesDir, CRASH_DIR).also { it.mkdirs() }

        // Eski logları temizle (max 5 tut)
        dir.listFiles()?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_LOGS - 1)?.forEach { it.delete() }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "crash_$timestamp.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        file.writeText(buildString {
            appendLine("=== AppOrganizer Crash Report ===")
            appendLine("Tarih: ${Date()}")
            appendLine("Cihaz: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
            appendLine("")
            appendLine("=== Stack Trace ===")
            appendLine(sw.toString())
        })

        Timber.e("Crash kaydedildi: ${file.absolutePath}")
    }

    fun getLastCrashLog(context: Context): String? {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists()) return null
        return dir.listFiles()
            ?.maxByOrNull { it.lastModified() }
            ?.readText()
    }

    fun getAllCrashLogs(context: Context): List<String> {
        val dir = File(context.filesDir, CRASH_DIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.readText() }
            ?: emptyList()
    }

    fun clearCrashLogs(context: Context) {
        File(context.filesDir, CRASH_DIR).deleteRecursively()
    }
}
