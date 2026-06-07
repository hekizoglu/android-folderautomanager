package com.armutlu.apporganizer.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.provider.Settings
import java.util.Calendar

object UsageStatsHelper {

    fun hasPermission(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, end)
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun openPermissionSettings(context: Context) {
        context.startActivity(
            android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Son N gündeki kullanım süresi (ms) — packageName → totalForegroundMs
    fun getUsageCounts(context: Context, days: Int = 30): Map<String, Long> {
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -days)
            val start = cal.timeInMillis

            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            stats?.groupBy { it.packageName }
                ?.mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
                ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
