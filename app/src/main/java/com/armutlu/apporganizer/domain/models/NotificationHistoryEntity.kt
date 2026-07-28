package com.armutlu.apporganizer.domain.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gerçek bildirim başlığı/metnini tutan geçmiş kaydı — yalnızca kullanıcı
 * AppPrefs.isNotificationTextEnabled() ile "Bildirim metnini göster" ayarını
 * AÇTIYSA doldurulur (varsayılan kapalı). Kapalıyken hiçbir başlık/metin
 * kaydedilmez; NotificationEvent (paket+zaman, içeriksiz) ayrı bir tablodur.
 * Retention: NotificationHistoryRetentionPolicy ile en fazla 7 gün / 500 kayıt.
 */
@Entity(
    tableName = "notification_history",
    indices = [Index("packageName"), Index("postedAt")]
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    @ColumnInfo(defaultValue = "''")
    val text: String = "",
    val postedAt: Long,
    @ColumnInfo(defaultValue = "0")
    val isRead: Boolean = false,
)
