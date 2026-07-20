package com.armutlu.apporganizer.domain.home

/**
 * [com.armutlu.apporganizer.data.local.HomeGridItemEntity.itemType] alanının domain karşılığı.
 * Faz S — S1 veri modeli, henüz hiçbir UI bunu tüketmiyor.
 */
enum class HomeGridItemType {
    SECTION,
    WIDGET,
    FOLDER,
    // Faz S2 — klasör içindeki tek bir uygulama kısayolunun serbest grid pozisyonu.
    APP_SHORTCUT,
}

/**
 * [HomeGridItemType] <-> Room'da saklanan ham String dönüşümü. Room entity'leri saf veri
 * taşımalı (enum bağımlılığı kurulmaz) — bu yüzden dönüşüm entity DIŞINDA, saf bir fonksiyon
 * çifti olarak burada yaşar (bkz. [TickerActionCodec] aynı pattern). [decode] bilinmeyen/bozuk
 * bir string için güvenle null döner — çağıran taraf kararını verir, sessizce varsayılan
 * uydurulmaz.
 */
object HomeGridItemTypeCodec {

    fun encode(type: HomeGridItemType): String = type.name

    fun decode(raw: String?): HomeGridItemType? {
        if (raw.isNullOrBlank()) return null
        return runCatching { HomeGridItemType.valueOf(raw) }.getOrNull()
    }
}
