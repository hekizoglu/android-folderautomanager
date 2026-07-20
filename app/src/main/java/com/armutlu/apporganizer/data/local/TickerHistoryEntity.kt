package com.armutlu.apporganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ticker haber şeridi (SmartTickerItem) için kalıcı arşiv kaydı — "tüm haberler" mail kutusu
 * ekranı için (Codex istisnası: AppDatabase'e yeni tablo — search kodu DEĞİŞMEDİ).
 *
 * [id] = [SmartTickerItem.dedupeKey] (suggestionKey ?: id) — aynı canlı ticker öğesi tekrar
 * üretildiğinde INSERT..IGNORE ile mevcut kayıt (ve okunma durumu) korunur, yeni satır AÇILMAZ.
 *
 * [actionType] — [TickerAction]'ı kayıp yapmadan geri çözülebilecek basit bir "wire format"
 * string'i (bkz. [TickerActionCodec]). Domain katmanına (TickerAction sealed interface) burada
 * bağımlılık kurulmaz — Room entity'leri saf veri taşımalı, bu yüzden encode/decode ayrı bir
 * saf fonksiyon çiftinde (TickerActionCodec) yaşar ve JVM testle doğrulanır.
 */
@Entity(tableName = "ticker_history")
data class TickerHistoryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val subtitle: String?,
    val icon: String,
    val createdAt: Long,
    val isRead: Boolean = false,
    val actionType: String,
    val sensitive: Boolean = false,
)
