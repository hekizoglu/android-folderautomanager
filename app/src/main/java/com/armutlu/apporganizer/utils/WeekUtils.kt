package com.armutlu.apporganizer.utils

import java.util.Calendar
import java.util.Locale

object WeekUtils {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun currentWeekStartEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(Locale("tr")).apply {
            timeInMillis = nowMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / DAY_MS
    }
}
