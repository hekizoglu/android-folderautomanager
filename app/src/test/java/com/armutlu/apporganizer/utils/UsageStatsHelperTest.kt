package com.armutlu.apporganizer.utils

import android.app.AppOpsManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Zaman-Kisitli Gorev — getScreenOnEventsInWindow() izin-yok yolunu dogrular. UsageStatsManager
 * gercek Android runtime gerektirdiginden (Robolectric yok bu modulde) burada sadece
 * hasPermission()==false erken-cikis yolu test edilir; MissionEngineTest tarafi evaluator
 * mantigini (found/not-found) saf Kotlin ile kapsar.
 */
class UsageStatsHelperTest {

    @Test
    fun `getScreenOnEventsInWindow izin yoksa null doner`() {
        val appOps = mockk<AppOpsManager>()
        every {
            appOps.unsafeCheckOpNoThrow(any(), any(), any())
        } returns AppOpsManager.MODE_ERRORED
        every {
            appOps.checkOpNoThrow(any(), any(), any())
        } returns AppOpsManager.MODE_ERRORED

        val context = mockk<Context>()
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOps
        every { context.packageName } returns "com.armutlu.apporganizer"

        val result = UsageStatsHelper.getScreenOnEventsInWindow(
            context = context,
            startHour = 23,
            endHour = 6,
            date = LocalDate.of(2026, 7, 20),
        )

        assertNull(result)
    }
}
