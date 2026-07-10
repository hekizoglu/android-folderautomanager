package com.armutlu.apporganizer.domain.usecase.usage

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class UsageEventType {
    RESUMED,
    PAUSED,
    STOPPED,
    KEYGUARD_SHOWN,
    KEYGUARD_HIDDEN,
    SCREEN_NON_INTERACTIVE,
    SCREEN_INTERACTIVE,
    DEVICE_SHUTDOWN,
}

data class UsageEvent(
    val packageName: String,
    val className: String,
    val eventType: UsageEventType,
    val timestamp: Long,
)

data class DailyPackageUsage(
    val localDate: String,
    val epochDay: Long,
    val packageName: String,
    val launchCount: Int,
    val foregroundDurationMs: Long,
    val hourlyForegroundMs: List<Long>,
    val globalForegroundMs: Long,
    val isPartial: Boolean,
)

/** Pure, deterministic conversion of lifecycle events into civil-day usage totals. */
class UsageSessionAggregator(private val zoneId: ZoneId) {
    fun aggregate(
        events: List<UsageEvent>,
        rangeStartMs: Long,
        rangeEndMs: Long,
    ): List<DailyPackageUsage> {
        require(rangeEndMs >= rangeStartMs) { "rangeEndMs must not precede rangeStartMs" }
        if (rangeEndMs == rangeStartMs) return emptyList()

        val active = linkedMapOf<String, MutableSet<String>>()
        val days = linkedMapOf<DayPackageKey, MutableUsage>()
        val globalByDay = linkedMapOf<Long, Long>()
        var keyguardShown = false
        var screenInteractive = true
        var cursor = rangeStartMs
        var startBoundaryCaptured = false
        var openAtStart: Set<String> = emptySet()

        val sorted = events.withIndex().sortedWith(
            compareBy<IndexedValue<UsageEvent>> { it.value.timestamp }
                .thenBy { eventOrder(it.value.eventType) }
                .thenBy { it.index },
        )

        fun available() = !keyguardShown && screenInteractive

        fun accrue(from: Long, to: Long) {
            if (to <= from || !available()) return
            val packages = active.filterValues { it.isNotEmpty() }.keys
            if (packages.isEmpty()) return
            splitByLocalHour(from, to) { start, end, dateTime ->
                val duration = end - start
                val epochDay = dateTime.toLocalDate().toEpochDay()
                globalByDay[epochDay] = (globalByDay[epochDay] ?: 0L) + duration
                packages.forEach { packageName ->
                    val usage = days.getOrPut(DayPackageKey(epochDay, packageName)) { MutableUsage() }
                    usage.duration += duration
                    usage.hourly[dateTime.hour] += duration
                }
            }
        }

        sorted.forEach { indexed ->
            val event = indexed.value
            if (!startBoundaryCaptured && event.timestamp >= rangeStartMs) {
                openAtStart = active.filterValues { it.isNotEmpty() }.keys.toSet()
                startBoundaryCaptured = true
            }
            val eventTime = event.timestamp.coerceIn(rangeStartMs, rangeEndMs)
            if (event.timestamp >= rangeStartMs) {
                accrue(cursor, eventTime)
                cursor = maxOf(cursor, eventTime)
            }

            // Events beyond the requested interval cannot affect its result.
            if (event.timestamp > rangeEndMs) return@forEach
            when (event.eventType) {
                UsageEventType.RESUMED -> {
                    if (event.packageName.isBlank() || event.className.isBlank()) return@forEach
                    val packageActivities = active.getOrPut(event.packageName) { linkedSetOf() }
                    val wasInactive = packageActivities.isEmpty()
                    if (packageActivities.add(event.className) && wasInactive && event.timestamp in rangeStartMs until rangeEndMs) {
                        val localDate = Instant.ofEpochMilli(event.timestamp).atZone(zoneId).toLocalDate()
                        days.getOrPut(DayPackageKey(localDate.toEpochDay(), event.packageName)) { MutableUsage() }.launches++
                    }
                }
                UsageEventType.PAUSED, UsageEventType.STOPPED -> {
                    active[event.packageName]?.remove(event.className)
                    if (active[event.packageName]?.isEmpty() == true) active.remove(event.packageName)
                }
                UsageEventType.KEYGUARD_SHOWN -> keyguardShown = true
                UsageEventType.KEYGUARD_HIDDEN -> keyguardShown = false
                UsageEventType.SCREEN_NON_INTERACTIVE -> screenInteractive = false
                UsageEventType.SCREEN_INTERACTIVE -> screenInteractive = true
                UsageEventType.DEVICE_SHUTDOWN -> {
                    active.clear()
                }
            }
        }

        if (!startBoundaryCaptured) openAtStart = active.filterValues { it.isNotEmpty() }.keys.toSet()
        accrue(cursor, rangeEndMs)
        val firstDate = Instant.ofEpochMilli(rangeStartMs).atZone(zoneId).toLocalDate()
        openAtStart.forEach { packageName ->
            days.getOrPut(DayPackageKey(firstDate.toEpochDay(), packageName)) { MutableUsage() }.partial = true
        }
        val openAtEnd = active.filterValues { it.isNotEmpty() }.keys
        openAtEnd.forEach { packageName ->
            val lastInstant = Instant.ofEpochMilli(rangeEndMs - 1).atZone(zoneId).toLocalDate()
            days.getOrPut(DayPackageKey(lastInstant.toEpochDay(), packageName)) { MutableUsage() }.partial = true
        }

        return days.entries
            .sortedWith(compareBy({ it.key.epochDay }, { it.key.packageName }))
            .map { (key, usage) ->
                val date = java.time.LocalDate.ofEpochDay(key.epochDay)
                DailyPackageUsage(
                    localDate = date.toString(),
                    epochDay = key.epochDay,
                    packageName = key.packageName,
                    launchCount = usage.launches,
                    foregroundDurationMs = usage.duration,
                    hourlyForegroundMs = usage.hourly.toList(),
                    globalForegroundMs = globalByDay[key.epochDay] ?: 0L,
                    isPartial = usage.partial,
                )
            }
    }

    private fun splitByLocalHour(from: Long, to: Long, consume: (Long, Long, ZonedDateTime) -> Unit) {
        var cursor = from
        while (cursor < to) {
            val local = Instant.ofEpochMilli(cursor).atZone(zoneId)
            val nextBoundary = local.plusHours(1).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
            val end = minOf(to, maxOf(cursor + 1, nextBoundary))
            consume(cursor, end, local)
            cursor = end
        }
    }

    private data class DayPackageKey(val epochDay: Long, val packageName: String)
    private class MutableUsage {
        var launches = 0
        var duration = 0L
        val hourly = LongArray(24)
        var partial = false
    }

    private companion object {
        fun eventOrder(type: UsageEventType): Int = when (type) {
            UsageEventType.PAUSED, UsageEventType.STOPPED, UsageEventType.DEVICE_SHUTDOWN,
            UsageEventType.KEYGUARD_SHOWN, UsageEventType.SCREEN_NON_INTERACTIVE -> 0
            UsageEventType.KEYGUARD_HIDDEN, UsageEventType.SCREEN_INTERACTIVE -> 1
            UsageEventType.RESUMED -> 2
        }
    }
}
