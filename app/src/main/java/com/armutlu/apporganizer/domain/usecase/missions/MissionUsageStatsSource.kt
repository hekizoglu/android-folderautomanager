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
    /**
     * İzin yoksa veya event verisi yoksa null doner.
     * [nowMillis] — Dongu M04: settlement gecmis donemleri degerlendirirken pencereyi donemin
     * bitisine (periodEndAt) sabitler; sistem "su an" hala pencere disina veri sizdirmasin diye.
     * Varsayilan `System.currentTimeMillis()` M02 cagri yerlerini bozmaz.
     */
    fun getDailySessionUsage(
        context: Context,
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<DailyPackageUsage>?

    /** İzin yoksa veya SDK < P ise null doner. [nowMillis] — bkz. [getDailySessionUsage]. */
    fun getUnlockCount(
        context: Context,
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int?

    /**
     * Dongu G1 (kisisel gorev hedefi) — kilit acma sayisini epochDay -> adet olarak doner.
     * Izin yoksa veya SDK < P ise null doner.
     */
    fun getUnlockCountPerDay(
        context: Context,
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Map<Long, Int>?
}

/** Uretimde kullanilan varsayilan implementasyon — dogrudan [UsageStatsHelper]'a delege eder. */
class DefaultMissionUsageStatsSource : MissionUsageStatsSource {
    override fun getDailySessionUsage(context: Context, days: Int, nowMillis: Long): List<DailyPackageUsage>? =
        (UsageStatsHelper.getDailySessionUsage(context, days = days, nowMillis = nowMillis)
            as? UsageStatsHelper.DailySessionResult.Available)?.days

    override fun getUnlockCount(context: Context, days: Int, nowMillis: Long): Int? =
        UsageStatsHelper.getUnlockCount(context, days = days, nowMillis = nowMillis)

    override fun getUnlockCountPerDay(context: Context, days: Int, nowMillis: Long): Map<Long, Int>? =
        UsageStatsHelper.getUnlockCountPerDay(context, days = days, nowMillis = nowMillis)
}
