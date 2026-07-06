package com.armutlu.apporganizer.domain.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bildirim olay kaydı — her gelen bildirim için bir satır (paket + zaman damgası).
 * Bildirim Analiz Raporu bu tablodan beslenir: "Instagram 200 bildirim gönderdi",
 * gece rahatsız edenler, dikkat dağıtanlar. 30 günden eski kayıtlar otomatik silinir.
 */
@Entity(
    tableName = "notification_events",
    indices = [Index("packageName"), Index("postedAt")]
)
data class NotificationEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val postedAt: Long,
)
