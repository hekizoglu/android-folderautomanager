package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Ağ veya ağır ML modeli kullanmadan bildirimi cihaz üzerinde sınıflandırır.
 *
 * Kurallar bilinçli olarak saf ve deterministiktir: aynı girdi her cihazda aynı sonucu üretir,
 * servis/UI bağımlılığı yoktur ve birim testte doğrudan çalıştırılabilir.
 */
class NotificationClassifierUseCase @Inject constructor() {

    fun classify(
        key: String,
        packageName: String,
        title: String,
        text: String,
        timestamp: Long,
        systemPriority: Int = 0,
    ): SmartNotification {
        val normalizedPackage = normalize(packageName)
        val normalizedContent = normalize("$title $text")
        val category = detectCategory(normalizedPackage, normalizedContent)
        val sensitive = category == NotificationCategory.FINANCE ||
            normalizedContent.containsAny(SENSITIVE_TERMS)
        val score = score(
            category = category,
            packageName = normalizedPackage,
            content = normalizedContent,
            systemPriority = systemPriority,
        )
        val suppress = category.suppressible && score < SUPPRESSION_SCORE_LIMIT

        return SmartNotification(
            key = key,
            packageName = packageName,
            title = title.trim(),
            text = text.trim(),
            category = category,
            importanceScore = score,
            timestamp = timestamp,
            isSensitive = sensitive,
            shouldSuppress = suppress,
        )
    }

    private fun detectCategory(packageName: String, content: String): NotificationCategory {
        return when {
            packageName.containsAny(FINANCE_PACKAGES) || content.containsAny(FINANCE_TERMS) ->
                NotificationCategory.FINANCE

            packageName.containsAny(DELIVERY_PACKAGES) || content.containsAny(DELIVERY_TERMS) ->
                NotificationCategory.DELIVERY

            content.containsAny(REMINDER_TERMS) -> NotificationCategory.REMINDER

            content.containsAny(PROMOTION_TERMS) -> NotificationCategory.PROMOTION

            packageName.containsAny(MESSAGING_PACKAGES) || content.containsAny(MESSAGING_TERMS) ->
                NotificationCategory.MESSAGING

            packageName.containsAny(SOCIAL_PACKAGES) || content.containsAny(SOCIAL_TERMS) ->
                NotificationCategory.SOCIAL

            packageName.containsAny(SYSTEM_PACKAGES) || content.containsAny(SYSTEM_TERMS) ->
                NotificationCategory.SYSTEM

            else -> NotificationCategory.OTHER
        }
    }

    private fun score(
        category: NotificationCategory,
        packageName: String,
        content: String,
        systemPriority: Int,
    ): Int {
        var result = category.defaultImportance
        result += systemPriority.coerceIn(-2, 2) * 6

        if (content.containsAny(URGENT_TERMS)) result += 18
        if (content.containsAny(SECURITY_TERMS)) result += 12
        if (packageName.containsAny(MESSAGING_PACKAGES) && category == NotificationCategory.MESSAGING) {
            result += 5
        }
        if (content.containsAny(PROMOTION_TERMS)) result -= 18
        if (content.containsAny(LOW_VALUE_TERMS)) result -= 8

        return result.coerceIn(0, 100)
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE, " ")
            .trim()
    }

    private fun String.containsAny(values: Set<String>): Boolean = values.any(::contains)

    private companion object {
        const val SUPPRESSION_SCORE_LIMIT = 40
        val COMBINING_MARKS = Regex("\\p{Mn}+")
        val WHITESPACE = Regex("\\s+")

        val FINANCE_PACKAGES = setOf(
            "akbank", "garanti", "yapikredi", "isbank", "ziraat", "vakifbank",
            "halkbank", "denizbank", "qnb", "enpara", "papara", "paycell",
        )
        val DELIVERY_PACKAGES = setOf(
            "trendyol", "hepsiburada", "amazon", "getir", "yemeksepeti", "migros",
            "ptt", "aras", "yurtici", "mng", "surat",
        )
        val MESSAGING_PACKAGES = setOf(
            "whatsapp", "telegram", "messaging", "messages", "facebook.orca", "signal",
        )
        val SOCIAL_PACKAGES = setOf(
            "instagram", "facebook", "twitter", "tiktok", "snapchat", "linkedin",
        )
        val SYSTEM_PACKAGES = setOf(
            "android", "systemui", "settings", "securitycenter", "permissioncontroller",
        )

        val FINANCE_TERMS = setOf(
            "bakiye", "hesap", "kartiniz", "kartınız", "harcama", "odeme", "ödeme",
            "transfer", "havale", "eft", "yatirim", "yatırım", "islem", "işlem",
            "dogrulama kodu", "doğrulama kodu", "tek kullanimlik", "tek kullanımlık",
        )
        val DELIVERY_TERMS = setOf(
            "kargo", "teslimat", "siparis", "sipariş", "kurye", "yola cikti", "yola çıktı",
            "dagitima cikti", "dağıtıma çıktı", "teslim edildi", "paketiniz",
        )
        val REMINDER_TERMS = setOf(
            "hatirlatici", "hatırlatıcı", "alarm", "takvim", "toplanti", "toplantı",
            "randevu", "etkinlik basliyor", "etkinlik başlıyor", "son tarih",
        )
        val PROMOTION_TERMS = setOf(
            "indirim", "kampanya", "firsat", "fırsat", "kupon", "sepette", "reklam",
            "sana ozel", "sana özel", "hemen al", "stoklarla sinirli", "stoklarla sınırlı",
        )
        val MESSAGING_TERMS = setOf(
            "yeni mesaj", "mesaj gonderdi", "mesaj gönderdi", "cevapladi", "cevapladı",
            "sana yazdi", "sana yazdı", "goruntulu arama", "görüntülü arama",
        )
        val SOCIAL_TERMS = setOf(
            "begendi", "beğendi", "takip etmeye basladi", "takip etmeye başladı",
            "yorum yapti", "yorum yaptı", "hikaye", "reels", "gonderini", "gönderini",
        )
        val SYSTEM_TERMS = setOf(
            "sistem", "guncelleme", "güncelleme", "pil", "depolama", "izin", "guvenlik", "güvenlik",
        )
        val URGENT_TERMS = setOf(
            "acil", "hemen", "simdi", "şimdi", "kritik", "son dakika", "gecikmis", "gecikmiş",
        )
        val SECURITY_TERMS = setOf(
            "guvenlik", "güvenlik", "supheli", "şüpheli", "giris", "giriş", "sifre", "şifre",
            "otp", "kod", "onay", "dogrulama", "doğrulama",
        )
        val SENSITIVE_TERMS = setOf(
            "bakiye", "hesap", "kart", "sifre", "şifre", "otp", "kod", "tutar", "₺", " tl",
        )
        val LOW_VALUE_TERMS = setOf(
            "bulten", "bülten", "onerilen", "önerilen", "sizin icin", "sizin için",
            "trend", "kesfet", "keşfet",
        )
    }
}
