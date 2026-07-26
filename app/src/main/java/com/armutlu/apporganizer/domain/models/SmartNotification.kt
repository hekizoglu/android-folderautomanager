package com.armutlu.apporganizer.domain.models

/**
 * Aktif bildirimin cihaz belleğinde yaşayan akıllı görünümü.
 *
 * [title] ve [text] kalıcı depolamaya yazılmaz. Servis kapandığında bu içerik bellekten düşer;
 * böylece sınıflandırma ve Hero özeti üretilebilirken hassas bildirim içeriği DB'ye sızmaz.
 */
data class SmartNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val category: NotificationCategory,
    val importanceScore: Int,
    val timestamp: Long,
    val isSensitive: Boolean,
    val shouldSuppress: Boolean,
)
