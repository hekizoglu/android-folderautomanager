package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Haftalik sinir hesaplarinin tek giris noktasi. Gercek hesaplama artik
 * [PeriodBoundaryResolver]'a (Dongu H01) delege edilir — ISO hafta (Pazartesi baslangicli),
 * DST-guvenli java.time tabanli. Dis davranis (donen deger: haftanin Pazartesi'sinin
 * [java.time.LocalDate.toEpochDay] degeri, sistem varsayilan saat dilimine gore) AYNI kalir;
 * bu yuzden mevcut cagiran taraflar (WeeklyGoalDao, AppListViewModel, WeeklyDigestWorker)
 * degismeden calismaya devam eder.
 */
object WeekUtils {

    fun currentWeekStartEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
        val zoneId = ZoneId.systemDefault()
        val clock = Clock.fixed(Instant.ofEpochMilli(nowMillis), zoneId)
        return PeriodBoundaryResolver(clock, zoneId).currentIsoWeek().weekStartEpochDay!!
    }
}
