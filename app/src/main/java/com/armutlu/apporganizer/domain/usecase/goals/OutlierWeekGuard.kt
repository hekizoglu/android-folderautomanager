package com.armutlu.apporganizer.domain.usecase.goals

/**
 * P2 — roadmap §4.5. Son haftanın kullanımı, son 4 tamamlanmış haftanın medyanına göre aşırı
 * sıra dışıysa (tatil, hastalık, telefon değişimi, UsageStats kesintisi) ham değer olarak
 * [AdaptiveCategoryTargetCalculator]'a geçirilmez — medyan etrafında makul bir aralığa
 * sınırlanır. Saf Kotlin, ayrı test edilebilir fonksiyon (roadmap zorunlu kılıyor).
 */
object OutlierWeekGuard {

    /** Son hafta, medyanın bu katından FAZLA ise (üst aykırı) medyan x bu kat kullanılır. */
    const val UPPER_MULTIPLIER = 1.75

    /** Son hafta, medyanın bu katından AZ ise (alt aykırı) medyan x bu kat kullanılır. */
    const val LOWER_MULTIPLIER = 0.60

    /**
     * @param lastWeekMinutes son (en yeni tamamlanmış) haftanın kullanım dakikası.
     * @param priorWeeksMinutes son haftadan ÖNCEKİ tamamlanmış haftaların kullanım dakikaları
     * (sırasız olabilir). 4'ten az veri varsa outlier koruması uygulanmaz — ham değer aynen döner
     * (roadmap: "son dört haftanın verisi varsa" koşulu).
     * @return outlier korumasından geçmiş, kullanıma hazır dakika değeri.
     */
    fun guard(lastWeekMinutes: Long, priorWeeksMinutes: List<Long>): Long {
        if (priorWeeksMinutes.size < 4) return lastWeekMinutes
        val median = median(priorWeeksMinutes)
        if (median <= 0.0) return lastWeekMinutes

        val upperBound = median * UPPER_MULTIPLIER
        val lowerBound = median * LOWER_MULTIPLIER

        return when {
            lastWeekMinutes > upperBound -> upperBound.toLong()
            lastWeekMinutes < lowerBound -> lowerBound.toLong()
            else -> lastWeekMinutes
        }
    }

    private fun median(values: List<Long>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid].toDouble()
        }
    }
}
