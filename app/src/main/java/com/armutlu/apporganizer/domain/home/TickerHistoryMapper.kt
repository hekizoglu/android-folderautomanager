package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.data.local.TickerHistoryEntity

/**
 * [SmartTickerItem] -> [TickerHistoryEntity] saf dönüşümü + 7 gün saklama süresi hesabı.
 * Ticker arşiv ekranı ("Tüm haberler", mail kutusu deneyimi) için — canlı ticker üretiminin
 * YANINDA, paralel bir kalıcı kayıt oluşturur (bkz. dosya başı notu: RealSmartTickerSource/
 * HomeTickerComposer davranışı DEĞİŞMEZ, bu sadece arşivleme).
 */
object TickerHistoryMapper {

    /** Arşiv kaydının saklama süresi — 7 gün (ms). */
    const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000

    fun toEntity(item: SmartTickerItem): TickerHistoryEntity = TickerHistoryEntity(
        id = item.dedupeKey,
        type = item.type.name,
        title = item.title,
        subtitle = item.subtitle,
        icon = item.icon,
        createdAt = item.createdAt,
        isRead = false,
        actionType = TickerActionCodec.encode(item.action),
        sensitive = item.sensitive,
    )

    fun toEntities(items: List<SmartTickerItem>): List<TickerHistoryEntity> = items.map(::toEntity)

    /** 7 gün önceki kesim zamanı — bu zamandan ESKİ kayıtlar [nowMillis]'te silinebilir. */
    fun cutoffMillis(nowMillis: Long): Long = nowMillis - RETENTION_MS
}
