package com.armutlu.apporganizer.domain.advice

import com.armutlu.apporganizer.domain.common.DataFreshness

/**
 * P7 — [DigitalAdviceEngine.evaluate] girdisi. TÜM alanlar mevcut akan verilerden türetilir
 * (roadmap §11 "yeni bir veri kaynağı EKLENMEZ" ilkesi — [com.armutlu.apporganizer.domain.home.TodayCardSelector]
 * ile aynı yaklaşım). Kategori/paket kimlikleri metne HARDCODE edilmez, sadece sayısal sinyal taşınır.
 */
data class DigitalAdviceInput(
    val permissionFreshness: DataFreshness,

    /** roadmap §11.1: hem yüzdesel hem mutlak eşik — herhangi biri tek başına yeterli DEĞİL. */
    val categoryUsageChanges: List<CategoryUsageChange> = emptyList(),

    /** En az bir aktif AUTO/MANUAL kategori hedefi gerçekte aşılmışsa. */
    val exceededCategoryGoalCount: Int = 0,

    /** Hafta içi projeksiyon riskindeki (AT_RISK, henüz aşılmamış) kategori hedefi sayısı. */
    val atRiskCategoryGoalCount: Int = 0,

    /** Tüm aktif kategori hedefleri hafta içi planında mı (olumlu pekiştirme sinyali). */
    val allCategoryGoalsOnTrack: Boolean = false,
    val hasAnyCategoryGoal: Boolean = false,

    /** Bildirimlerin en baskın kaynağının toplam içindeki payı (0f..1f), null = veri yok. */
    val notificationNoiseTopSourceShare: Float? = null,

    /** Sabah ilk 30dk içinde sosyal uygulama açılan gün sayısı (son 7 gün penceresi). */
    val morningSocialOpenDaysLast7: Int? = null,

    /** 23:00 sonrası kullanım gerçekleşen gün sayısı (son 7 gün penceresi). */
    val lateNightUsageDaysLast7: Int? = null,

    val unusedAppCount: Int = 0,
    val uncategorizedAppCount: Int = 0,

    /** Bu hafta tüm görevlerin/hedeflerin planında olduğu (olumlu tavsiye tetikleyicisi). */
    val everythingOnTrackThisWeek: Boolean = false,
)

/**
 * Kategori kullanım değişimi girdisi — kategori kimliği TAŞINMAZ, sadece yüzde/mutlak fark
 * ve önceden çözülmüş bir "etiket" string kaynağı (çağıran taraf hangi kategori olduğunu
 * bilir, motor bilmez — bu, roadmap'in "kategori kimlikleri metne hardcode edilmez" ilkesiyle
 * tutarlı: motor sadece SAYI üzerinden karar verir, adı UI katmanına aittir).
 */
data class CategoryUsageChange(
    val categoryNameRes: Int,
    val previousWeekMinutes: Long,
    val currentWeekMinutes: Long,
) {
    val percentChange: Float get() =
        if (previousWeekMinutes <= 0L) 0f else (currentWeekMinutes - previousWeekMinutes).toFloat() / previousWeekMinutes
    val absoluteChangeMinutes: Long get() = currentWeekMinutes - previousWeekMinutes
}
