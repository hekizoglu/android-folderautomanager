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

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(appContext, throwable)
            } catch (e: Exception) {
                Timber.e(e, "CrashReporter: log kaydetme hatası")
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Timber.d("CrashReporter kuruldu")
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
