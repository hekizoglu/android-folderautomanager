package com.armutlu.apporganizer.utils

import android.app.usage.UsageEvents
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

    // Her uygulamanın son kullanım zamanı (epoch ms) — packageName → lastTimeUsed
    fun getLastUsedTimes(context: Context, days: Int = 90): Map<String, Long> {
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -days)
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, end)
            stats?.groupBy { it.packageName }
                ?.mapValues { (_, list) -> list.maxOf { it.lastTimeUsed } }
                ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
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

    /**
     * Yaklaşım B — ağırlıklı öneri skoru: recency(0.4) + frequency(0.4) + timeSlot(0.2)
     *
     * Gece   06-11 → sabah, 11-14 → öğle, 14-18 → öğleden sonra, 18-06 → akşam/gece
     * Samsung/Xiaomi tuzağı: lastTimeUsed=0 → lastTimeStamp fallback
     *
     * @return packageName → score (0.0..1.0), sadece kullanım verisi olan paketler
     */
    fun getWeightedScores(context: Context, days: Int = 28): Map<String, Float> {
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val windowStart = now - days.toLong() * 24 * 3600 * 1000

            val currentSlot = timeSlot(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

            // Event bazlı veri — daha doğru frekans ve time-slot için
            val slotCounts = mutableMapOf<String, IntArray>()  // pkg → [sabah, öğle, öğleden sonra, akşam]
            val launchCounts = mutableMapOf<String, Int>()

            try {
                val events = manager.queryEvents(windowStart, now)
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                    val pkg = event.packageName
                    if (pkg.isNullOrEmpty()) continue
                    launchCounts[pkg] = (launchCounts[pkg] ?: 0) + 1
                    val slot = timeSlot(Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                        .get(Calendar.HOUR_OF_DAY))
                    val arr = slotCounts.getOrPut(pkg) { IntArray(4) }
                    arr[slot]++
                }
            } catch (_: Exception) {
                // queryEvents bazı ROM'larda izin verilmiş olsa da çalışmaz — sessizce devam et
            }

            // queryUsageStats: recency + frekans fallback (queryEvents boş geldiyse)
            val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, windowStart, now)
                ?: emptyList()

            val statsMap = stats.groupBy { it.packageName }.mapValues { (_, list) ->
                val lastUsed = list.maxOf {
                    // Samsung/Xiaomi: lastTimeUsed=0 → lastTimeStamp fallback
                    if (it.lastTimeUsed > 0) it.lastTimeUsed else it.lastTimeStamp
                }
                val totalMs = list.sumOf { it.totalTimeInForeground }
                Pair(lastUsed, totalMs)
            }

            val maxLaunch = launchCounts.values.maxOrNull()?.toFloat() ?: 1f
            val maxTotalMs = statsMap.values.maxOfOrNull { it.second }?.toFloat() ?: 1f

            val allPkgs = (statsMap.keys + launchCounts.keys).toSet()
            allPkgs.associateWith { pkg ->
                val (lastUsed, totalMs) = statsMap[pkg] ?: Pair(0L, 0L)

                // Recency (0.4)
                val ageMs = now - lastUsed
                val recency = when {
                    lastUsed <= 0L             -> 0f
                    ageMs < 86_400_000L        -> 1.0f   // < 24 saat
                    ageMs < 7 * 86_400_000L    -> 0.5f   // < 1 hafta
                    ageMs < 14 * 86_400_000L   -> 0.25f  // < 2 hafta
                    else                        -> 0.1f
                }

                // Frequency (0.4) — launch count varsa onu kullan, yoksa totalMs heuristic
                val frequency = if (launchCounts.isNotEmpty()) {
                    (launchCounts[pkg] ?: 0) / maxLaunch
                } else {
                    if (maxTotalMs > 0f) totalMs / maxTotalMs else 0f
                }

                // TimeSlot (0.2)
                val slots = slotCounts[pkg]
                val timeSlotScore = if (slots != null) {
                    val maxSlot = slots.maxOrNull()?.toFloat() ?: 1f
                    if (maxSlot > 0f) slots[currentSlot] / maxSlot else 0f
                } else 0f

                recency * 0.4f + frequency * 0.4f + timeSlotScore * 0.2f
            }.filter { it.value > 0f }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // 0=sabah(06-11), 1=öğle(11-14), 2=öğleden sonra(14-18), 3=akşam/gece(18-06)
    private fun timeSlot(hour: Int): Int = when (hour) {
        in 6..10  -> 0
        in 11..13 -> 1
        in 14..17 -> 2
        else      -> 3
    }
}
