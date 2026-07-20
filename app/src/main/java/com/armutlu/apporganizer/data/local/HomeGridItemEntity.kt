package com.armutlu.apporganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Faz S (Serbest Sürükle-Bırak Ana Ekran Sistemi) — S1 veri modeli. Ana ekranda serbest 2D
 * yerleşim (hücre bazlı grid) için kalıcı konum kaydı. Bu tablo şu an HİÇBİR UI tarafından
 * okunmuyor/yazılmıyor — mevcut 1D sistemler ([com.armutlu.apporganizer.domain.models.HomeLayoutItem],
 * widget alan listesi) DEĞİŞMEDİ, bu tamamen paralel/gelecekte kullanılacak bir altyapı.
 *
 * [itemId] — öğeyi benzersiz tanımlayan kaynak anahtarı: `HomeSectionId.name` (bölüm),
 * widget instance id'nin string hali (widget) veya `"folder_<categoryId>"` (klasör).
 * [itemType] — Room'da String olarak saklanır, domain katmanında [HomeGridItemType] enum'ına
 * çevrilir (bkz. [HomeGridItemTypeCodec]) — TickerHistoryEntity.type + TickerActionCodec
 * pattern'inin taklidi: Room entity'leri saf veri taşımalı, enum bağımlılığı burada kurulmaz.
 * [screenIndex] — hangi ana ekran sayfası (0 = Dashboard, 1+ = klasör sayfaları); şu an
 * kullanılmıyor, gelecekteki çoklu sayfa desteği için hazırlık.
 */
@Entity(tableName = "home_grid_items")
data class HomeGridItemEntity(
    @PrimaryKey val itemId: String,
    val itemType: String,
    val screenIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
)
