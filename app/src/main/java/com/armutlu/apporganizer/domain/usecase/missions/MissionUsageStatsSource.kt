package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import com.armutlu.apporganizer.utils.UsageStatsHelper

/**
 * Dongu M02 — [MissionMetricSnapshotProvider]'in ihtiyac duydugu iki `UsageStatsHelper`
 * cagrisini soyutlar (gunluk oturum verisi + kilit acma sayisi). `UsageStatsHelper` bir
 * `object` (Android statik cagrilari sarar) oldugundan dogrudan test edilemez; bu interface
 * sayesinde provider testlerde sahte (fake) bir kaynakla, uretimde gercek Android API'siyle
 * calisir. Buyuk bir UsageStatsHelper refactor'u YAPILMAZ — sadece bu iki cagri enjekte edilir.
 */
interface MissionUsageStatsSource {
    /** İzin yoksa veya event verisi yoksa null doner. */
    fun getDailySessionUsage(context: Context, days: Int): List<DailyPackageUsage>?

    /** İzin yoksa veya SDK < P ise null doner. */
    fun getUnlockCount(context: Context, days: Int): Int?
}

/** Uretimde kullanilan varsayilan implementasyon — dogrudan [UsageStatsHelper]'a delege eder. */
class DefaultMissionUsageStatsSource : MissionUsageStatsSource {
    override fun getDailySessionUsage(context: Context, days: Int): List<DailyPackageUsage>? =
        (UsageStatsHelper.getDailySessionUsage(context, days = days)
            as? UsageStatsHelper.DailySessionResult.Available)?.days

    override fun getUnlockCount(context: Context, days: Int): Int? =
        UsageStatsHelper.getUnlockCount(context, days = days)
}
