package com.armutlu.apporganizer.domain.common

/**
 * Ana ekranın tek bir veri kaynağından alabileceği sonucu tip güvenli şekilde
 * modelleyen sarmalayıcı — Döngü H04
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 602-637).
 *
 * Amaç: tek bir kaynak (pulse/mission/ticker) başarısız olduğunda ana ekranın
 * çökmemesi veya tamamen boşalmaması — koordinatör her kaynağı bu tipe sararak
 * durumu (başarılı / bayat ama kullanılabilir / eksik / başarısız) açıkça taşır.
 */
sealed interface HomeDataResult<out T> {

    /** Kaynak bu turda başarıyla tazelendi, [value] güncel. */
    data class Ready<T>(val value: T) : HomeDataResult<T>

    /**
     * Kaynak bu turda tazelenemedi ama önceki başarılı [value] hâlâ gösterilebilir.
     * [warningCode] neden bayat kaldığını sabit kod olarak taşır ([HomeErrorCodes]).
     */
    data class Stale<T>(val value: T, val warningCode: String) : HomeDataResult<T>

    /** Kaynaktan hiç değer alınamadı ama bu bir hata değil — [reason] açıklar. */
    data class Missing(val reason: MissingReason) : HomeDataResult<Nothing>

    /** Kaynak hata verdi ve gösterilecek önceki bir değer de yok. */
    data class Failed(val errorCode: String) : HomeDataResult<Nothing>
}

/**
 * [HomeDataResult.Ready] veya [HomeDataResult.Stale] ise taşınan değeri döner,
 * [HomeDataResult.Missing]/[HomeDataResult.Failed] için null döner.
 */
fun <T> HomeDataResult<T>.valueOrNull(): T? = when (this) {
    is HomeDataResult.Ready -> value
    is HomeDataResult.Stale -> value
    is HomeDataResult.Missing -> null
    is HomeDataResult.Failed -> null
}

/**
 * UI'ın bu sonucu gösterip gösteremeyeceğini belirtir — [Ready] ve [Stale] kullanılabilir,
 * [Missing] ve [Failed] gösterilecek veri taşımaz.
 */
fun <T> HomeDataResult<T>.isUsable(): Boolean = when (this) {
    is HomeDataResult.Ready -> true
    is HomeDataResult.Stale -> true
    is HomeDataResult.Missing -> false
    is HomeDataResult.Failed -> false
}
