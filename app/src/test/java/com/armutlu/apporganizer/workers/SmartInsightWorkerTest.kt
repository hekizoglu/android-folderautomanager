package com.armutlu.apporganizer.workers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SmartInsightWorkerTest {

    @Test
    fun `calculateInitialDelayMs returns same-day delay when target is later today`() {
        val now = calendarAt(hour = 10, minute = 15)

        val delay = SmartInsightWorker.calculateInitialDelayMs(
            targetHour = 20,
            targetMinute = 0,
            nowMillis = now,
        )

        assertEquals(9 * 60 + 45, delay / 60_000)
    }

    @Test
    fun `calculateInitialDelayMs rolls to next day when target time already passed`() {
        val now = calendarAt(hour = 20, minute = 30)

        val delay = SmartInsightWorker.calculateInitialDelayMs(
            targetHour = 20,
            targetMinute = 0,
            nowMillis = now,
        )

        assertEquals(23 * 60 + 30, delay / 60_000)
    }

    private fun calendarAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
