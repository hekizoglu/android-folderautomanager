package com.armutlu.apporganizer.domain.home

/** Sums package notification counts without allowing bad input or Int overflow to leak to UI. */
fun safeRecentNotificationTotal(counts: Map<String, Int>): Int = counts.values.fold(0L) { total, count ->
    (total + count.coerceAtLeast(0).toLong()).coerceAtMost(Int.MAX_VALUE.toLong())
}.toInt()
