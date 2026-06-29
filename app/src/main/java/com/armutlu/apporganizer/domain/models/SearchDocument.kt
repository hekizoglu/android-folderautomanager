package com.armutlu.apporganizer.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * FTS5 araması için ortak döküman modeli.
 * Her aranabilir varlık (app, kategori, kişi, dosya) bu şemaya normalize edilir.
 *
 * Bu tablo FTS5 sanal tablosunun "content=" ile bağlı olduğu gölge tablodur —
 * FTS sorgusu bu tablodan doğrudan JOIN'siz okuma yapar.
 */
@Entity(tableName = "search_documents")
data class SearchDocument(
    @PrimaryKey(autoGenerate = true)
    val docId: Int = 0,

    /** "app" | "category" | "contact" | "file" */
    @ColumnInfo(name = "source_type")
    val sourceType: String,

    /** packageName | categoryId | contactId | fileUri */
    @ColumnInfo(name = "source_id")
    val sourceId: String,

    /** Görünen isim (appName, DISPLAY_NAME, vs.) */
    val title: String,

    /** Alt bilgi (paket adı, telefon, dosya yolu). Kategoride boş. */
    val subtitle: String = "",

    /** Ikon lookup key (packageName / "category:{id}" / "contact:{uri}" / mime icon) */
    @ColumnInfo(name = "icon_key")
    val iconKey: String,

    /** Grup sıralaması: "app" (öncelikli), "category", "contact", "file" */
    @ColumnInfo(name = "source_group")
    val sourceGroup: String,

    /** Son değişiklik epoch millis — recency bonus için */
    @ColumnInfo(name = "last_modified")
    val lastModified: Long
)

/** Kaynak tipi enum — sorgu sonrası gruplandırma için */
enum class SourceType(val key: String, val groupOrder: Int) {
    APP("app", 0),
    CATEGORY("category", 1),
    CONTACT("contact", 2),
    FILE("file", 3);

    companion object {
        fun fromKey(key: String): SourceType = entries.first { it.key == key }
    }
}
