package com.armutlu.apporganizer.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import com.armutlu.apporganizer.domain.usecase.usage.UsageEvent
import com.armutlu.apporganizer.domain.usecase.usage.UsageEventType
import com.armutlu.apporganizer.domain.usecase.usage.UsageSessionAggregator
import java.time.ZoneId
import java.util.Calendar

object UsageStatsHelper {

    sealed interface DailySessionResult {
        data class Available(val days: List<DailyPackageUsage>) : DailySessionResult
        data class Unavailable(val reason: Reason) : DailySessionResult

        enum class Reason { PERMISSION_DENIED, NO_EVENT_DATA, QUERY_FAILED }
    }

    @Suppress("DEPRECATION")
    fun hasPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * UsageEvents olaylarını cihaz dışına çıkarmadan günlük paket oturumlarına dönüştürür.
     * Sistem olayları yalnız birkaç gün saklayabildiği için eski/boş pencere "0 kullanım" sayılmaz.
     */
    fun getDailySessionUsage(
        context: Context,
        days: Int = 7,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DailySessionResult {
        require(days > 0) { "days must be positive" }
        if (!hasPermission(context)) {
            return DailySessionResult.Unavailable(DailySessionResult.Reason.PERMISSION_DENIED)
        }

        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val today = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
            val rangeStart = today.minusDays(days.toLong() - 1L)
                .atStartOfDay(zoneId).toInstant().toEpochMilli()
            val replayStart = today.minusDays(days.toLong())
                .atStartOfDay(zoneId).toInstant().toEpochMilli()
            val usageEvents = manager.queryEvents(replayStart, nowMillis)
                ?: return DailySessionResult.Unavailable(DailySessionResult.Reason.NO_EVENT_DATA)
            val mapped = buildList {
                val event = UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    val type = event.eventType.toDomainEventType() ?: continue
                    add(
                        UsageEvent(
                            packageName = event.packageName.orEmpty(),
                            className = event.className ?: event.packageName.orEmpty(),
                            eventType = type,
                            timestamp = event.timeStamp,
                        ),
                    )
                }
            }
            if (mapped.isEmpty()) {
                DailySessionResult.Unavailable(DailySessionResult.Reason.NO_EVENT_DATA)
            } else {
                DailySessionResult.Available(
                    UsageSessionAggregator(zoneId).aggregate(mapped, rangeStart, nowMillis),
                )
            }
        } catch (_: SecurityException) {
            DailySessionResult.Unavailable(DailySessionResult.Reason.PERMISSION_DENIED)
        } catch (_: Exception) {
            DailySessionResult.Unavailable(DailySessionResult.Reason.QUERY_FAILED)
        }
    }

    fun openPermissionSettings(context: Context) {
        context.startActivity(
            android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Her uygulamanın son kullanım zamanı (epoch ms) — packageName → lastTimeUsed
    fun getUnlockCount(
        context: Context,
        days: Int = 7,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val windowStart = nowMillis - days.toLong() * 24 * 60 * 60 * 1000
            val events = manager.queryEvents(windowStart, nowMillis) ?: return null
            val event = UsageEvents.Event()
            var count = 0
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) count++
            }
            count
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Dongu G1 (kisisel gorev hedefi) — kilit acma sayisini gun bazinda gruplar. [getUnlockCount]
     * gibi ayni KEYGUARD_HIDDEN olayini kullanir, sadece epochDay'e gore ayristirir.
     * Izin yoksa veya SDK < P ise null doner.
     */
    fun getUnlockCountPerDay(
        context: Context,
        days: Int = 7,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Map<Long, Int>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val windowStart = nowMillis - days.toLong() * 24 * 60 * 60 * 1000
            val events = manager.queryEvents(windowStart, nowMillis) ?: return null
            val event = UsageEvents.Event()
            val counts = mutableMapOf<Long, Int>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                    val epochDay = java.time.Instant.ofEpochMilli(event.timeStamp)
                        .atZone(zoneId).toLocalDate().toEpochDay()
                    counts[epochDay] = (counts[epochDay] ?: 0) + 1
                }
            }
            counts
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Zaman-Kisitli Gorev — [date] gununde [startHour]:00 ile [endHour]:00 arasinda (endHour <
     * startHour ise gece yarisini gecen pencere, orn. 23-6) ekran acilma (SCREEN_INTERACTIVE)
     * olayi var mi. DAILY_NO_LATE_NIGHT'in sabit 23:00-06:00 mantiginin genellenmis hali —
     * queryEvents + SCREEN_INTERACTIVE filtreleme kullanir (ayni desen: getUnlockCount vb.).
     * Izin yoksa veya event verisi alinamazsa null doner (veri-yok ile "kullanim yok" ayrimi
     * MissionEngine.evaluateNoUsageInWindow'da korunur).
     */
    fun getScreenOnEventsInWindow(
        context: Context,
        startHour: Int,
        endHour: Int,
        date: java.time.LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean? {
        if (!hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val windowStartMillis = date.atTime(startHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            val windowEndMillis = if (endHour <= startHour) {
                date.plusDays(1).atTime(endHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            } else {
                date.atTime(endHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            }
            val nowMillis = System.currentTimeMillis()
            if (windowStartMillis > nowMillis) return false
            val queryEnd = windowEndMillis.coerceAtMost(nowMillis)
            val events = manager.queryEvents(windowStartMillis, queryEnd) ?: return null
            val event = UsageEvents.Event()
            var found = false
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE) {
                    found = true
                    break
                }
            }
            found
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

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
                    if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
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

    /**
     * "Bu saatte en çok kullandıkların" — şu anki saat dilimindeki MUTLAK başlatma sayısına
     * göre sıralı paket listesi (son [days] gün). getWeightedScores'tan farkı: burada saat dilimi
     * tek ölçüt ve app-içi değil app-LERARASI mutlak sayım — gerçekten bu saatte en çok açtıkların.
     * queryEvents desteklenmiyorsa boş liste döner (çağıran taraf fallback yapar).
     */
    fun getCurrentSlotTopApps(context: Context, days: Int = 28): List<String> {
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val windowStart = now - days.toLong() * 24 * 3600 * 1000
            val currentSlot = timeSlot(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            val slotCounts = mutableMapOf<String, Int>()
            val events = manager.queryEvents(windowStart, now)
            val event = UsageEvents.Event()
            val cal = Calendar.getInstance()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
                val pkg = event.packageName ?: continue
                cal.timeInMillis = event.timeStamp
                if (timeSlot(cal.get(Calendar.HOUR_OF_DAY)) == currentSlot) {
                    slotCounts[pkg] = (slotCounts[pkg] ?: 0) + 1
                }
            }
            slotCounts.entries.sortedByDescending { it.value }.map { it.key }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Son 24 saatin saatlik toplam ekran kullanımı (dakika) — 24 eleman, index 0 = 24 saat önce,
     * index 23 = içinde bulunulan saat. Ekran süresi RESUMED→PAUSED/STOPPED oturumlarından
     * hesaplanır; saat sınırını aşan oturumlar ilgili kovalara bölünür.
     * İzin yoksa veya event verisi alınamazsa null döner.
     */
    fun getHourlyUsageLast24h(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<Int>? {
        if (!hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val hourMs = 60L * 60 * 1000
            val windowStart = nowMillis - 24 * hourMs
            val events = manager.queryEvents(windowStart, nowMillis) ?: return null
            val bucketsMs = LongArray(24)

            fun addSession(startMs: Long, endMs: Long) {
                var cursor = startMs.coerceAtLeast(windowStart)
                val sessionEnd = endMs.coerceAtMost(nowMillis)
                while (cursor < sessionEnd) {
                    val bucket = (((cursor - windowStart) / hourMs).toInt()).coerceIn(0, 23)
                    val bucketEnd = windowStart + (bucket + 1) * hourMs
                    val chunkEnd = minOf(sessionEnd, bucketEnd)
                    bucketsMs[bucket] += chunkEnd - cursor
                    cursor = chunkEnd
                }
            }

            val event = UsageEvents.Event()
            var foregroundPkg: String? = null
            var foregroundSince = 0L
            var sawEvent = false
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        sawEvent = true
                        // Aynı anda tek ön plan uygulaması olur — öncekini kapat
                        if (foregroundPkg != null) addSession(foregroundSince, event.timeStamp)
                        foregroundPkg = event.packageName
                        foregroundSince = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED,
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.DEVICE_SHUTDOWN -> {
                        if (foregroundPkg != null &&
                            (event.packageName == foregroundPkg || event.packageName.isNullOrEmpty())
                        ) {
                            addSession(foregroundSince, event.timeStamp)
                            foregroundPkg = null
                        }
                    }
                }
            }
            // Hâlâ açık oturum varsa "şimdi"ye kadar say
            if (foregroundPkg != null) addSession(foregroundSince, nowMillis)
            if (!sawEvent) return null
            bucketsMs.map { (it / 60_000L).toInt().coerceAtMost(60) }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    // 0=sabah(06-11), 1=öğle(11-14), 2=öğleden sonra(14-18), 3=akşam/gece(18-06)
    private fun timeSlot(hour: Int): Int = when (hour) {
        in 6..10  -> 0
        in 11..13 -> 1
        in 14..17 -> 2
        else      -> 3
    }

    /** Saat -> dilim (0..3). Çağıranların cache'i dilim değişince yenilemesi için public. */
    fun slotOf(hour: Int): Int = timeSlot(hour)

    private fun Int.toDomainEventType(): UsageEventType? = when (this) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.RESUMED
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.PAUSED
        UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventType.STOPPED
        UsageEvents.Event.KEYGUARD_SHOWN -> UsageEventType.KEYGUARD_SHOWN
        UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEventType.KEYGUARD_HIDDEN
        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_NON_INTERACTIVE
        UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.SCREEN_INTERACTIVE
        UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventType.DEVICE_SHUTDOWN
        else -> null
    }
}
