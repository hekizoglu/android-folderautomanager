package com.armutlu.apporganizer.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bildirim olay kaydı — yalnız içeriksiz analiz metadata'sı tutulur.
 * Başlık, metin, gönderen, OTP, tutar veya hesap bilgisi bu entity'ye eklenmez.
 */
@Entity(
    tableName = "notification_events",
    indices = [Index("packageName"), Index("postedAt")],
)
data class NotificationEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val postedAt: Long,
    @ColumnInfo(defaultValue = "'OTHER'")
    val category: String = NotificationCategory.OTHER.name,
    @ColumnInfo(defaultValue = "35")
    val importanceScore: Int = NotificationCategory.OTHER.defaultImportance,
    @ColumnInfo(defaultValue = "0")
    val wasSuppressed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val systemPriority: Int = 0,
)
