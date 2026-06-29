package com.armutlu.apporganizer.utils

import androidx.compose.ui.graphics.Color
import com.armutlu.apporganizer.domain.models.Category

/**
 * Bildirim badge'inin rengini kategori + paket adına göre belirler.
 * İçerik okumaz — yalnızca kategori ve paket öneki kullanır.
 *
 * Kırmızı : Çağrı / Alarm / Finans uyarısı
 * Yeşil   : Mesajlaşma / Sosyal mesaj
 * Sarı    : Güncelleme / Sistem / Haber
 * Gri     : Diğer / Düşük önem
 */
object BadgeColorEngine {

    val Red    = Color(0xFFE53935)
    val Green  = Color(0xFF43A047)
    val Yellow = Color(0xFFFDD835)
    val Grey   = Color(0xFF9E9E9E)

    private val messagingCategories = setOf(
        Category.CAT_COMMUNICATION,
        Category.CAT_SOCIAL,
        Category.CAT_DATING
    )

    private val alertCategories = setOf(
        Category.CAT_FINANCE
    )

    private val updateCategories = setOf(
        Category.CAT_NEWS,
        Category.CAT_UTILITIES,
        Category.CAT_GOOGLE,
        Category.CAT_SAMSUNG,
        Category.CAT_MICROSOFT
    )

    // Paket adı önekine göre ek tanıma
    private val alarmPkgPrefixes = listOf(
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.samsung.android.app.clockpackage",
        "com.oneplus.clock",
        "com.miui.clock",
        "com.coloros.alarmclock"
    )

    private val messagingPkgPrefixes = listOf(
        "com.whatsapp",
        "org.telegram",
        "com.discord",
        "com.facebook.orca",
        "com.instagram",
        "com.snapchat",
        "com.twitter",
        "com.linkedin",
        "com.tiktok",
        "jp.naver.line",
        "com.viber",
        "com.skype",
        "com.microsoft.teams",
        "com.slack",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    private val updatePkgPrefixes = listOf(
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    fun badgeColor(categoryId: String, packageName: String): Color {
        // Paket adı tabanlı doğrudan eşleşme — kategoriden daha doğru
        if (alarmPkgPrefixes.any { packageName.startsWith(it) }) return Red
        if (messagingPkgPrefixes.any { packageName.startsWith(it) }) return Green
        if (updatePkgPrefixes.any { packageName.startsWith(it) }) return Yellow

        // Kategori tabanlı eşleşme
        return when (categoryId) {
            in messagingCategories -> Green
            in alertCategories     -> Red
            in updateCategories    -> Yellow
            else                   -> Red
        }
    }
}
