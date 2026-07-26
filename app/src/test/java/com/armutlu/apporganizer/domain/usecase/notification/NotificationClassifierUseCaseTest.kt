package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationClassifierUseCaseTest {
    private val classifier = NotificationClassifierUseCase()

    @Test
    fun `classifies Turkish and English fixtures with collision rules`() {
        fixtures.forEachIndexed { index, fixture ->
            val result = classifier.classify(
                key = "fixture-$index",
                packageName = fixture.pkg,
                title = fixture.title,
                text = fixture.text,
                timestamp = index.toLong(),
                systemPriority = fixture.priority,
            )
            assertEquals(fixture.name, fixture.category, result.category)
            assertTrue("${fixture.name}: score must stay in 0..100", result.importanceScore in 0..100)
        }
    }

    @Test
    fun `critical authentication messages are sensitive high priority and visible`() {
        listOf(
            f("Akbank OTP", "com.akbank.android.apps.akbank_direkt", "Güvenlik doğrulaması", "123456 tek kullanımlık doğrulama kodunuz", NotificationCategory.FINANCE, 1),
            f("Shopping verification", "com.trendyol.android", "Ödeme onayı", "Verification code: 774411", NotificationCategory.FINANCE, 1),
            f("Instagram login code", "com.instagram.android", "Login alert", "Your login code is 123456", NotificationCategory.FINANCE, 1),
        ).forEachIndexed { index, fixture ->
            val result = classifier.classify("auth-$index", fixture.pkg, fixture.title, fixture.text, index.toLong(), fixture.priority)
            assertEquals(NotificationCategory.FINANCE, result.category)
            assertTrue(result.isSensitive)
            assertTrue("${fixture.name}: expected high priority", result.importanceScore >= 80)
            assertFalse(result.shouldSuppress)
        }
    }

    @Test
    fun `pure promotions remain below suppression threshold`() {
        listOf(
            f("Trendyol promotion", "com.trendyol.android", "Sana özel fırsat", "Sepette yüzde 50 indirim", NotificationCategory.PROMOTION),
            f("Bank promotion", "com.akbank.android.apps.akbank_direkt", "Kampanya", "Sana özel indirim fırsatı", NotificationCategory.PROMOTION),
            f("Instagram promotion", "com.instagram.android", "Special offer", "Limited stock, buy now", NotificationCategory.PROMOTION),
        ).forEachIndexed { index, fixture ->
            val result = classifier.classify("promo-$index", fixture.pkg, fixture.title, fixture.text, index.toLong())
            assertEquals(NotificationCategory.PROMOTION, result.category)
            assertTrue(result.shouldSuppress)
            assertTrue("${fixture.name}: expected score below 40", result.importanceScore < 40)
        }
    }

    @Test
    fun `word and package boundaries prevent substring false positives`() {
        val wholesale = classifier.classify("sale-boundary", "com.example.shop", "Wholesale report", "Monthly wholesale report", 1L)
        val clone = classifier.classify("package-boundary", "com.example.whatsappclone", "Hello", "General information", 2L)
        assertEquals(NotificationCategory.OTHER, wholesale.category)
        assertEquals(NotificationCategory.OTHER, clone.category)
    }

    private data class Fixture(
        val name: String,
        val pkg: String,
        val title: String,
        val text: String,
        val category: NotificationCategory,
        val priority: Int = 0,
    )

    private companion object {
        fun f(name: String, pkg: String, title: String, text: String, category: NotificationCategory, priority: Int = 0) =
            Fixture(name, pkg, title, text, category, priority)

        val fixtures = listOf(
            f("Akbank OTP", "com.akbank.android.apps.akbank_direkt", "Güvenlik doğrulaması", "123456 tek kullanımlık doğrulama kodunuz", NotificationCategory.FINANCE, 1),
            f("Shopping verification", "com.trendyol.android", "Ödeme onayı", "Verification code: 774411", NotificationCategory.FINANCE),
            f("Instagram login code", "com.instagram.android", "Login alert", "Your login code is 123456", NotificationCategory.FINANCE),
            f("Card transaction", "com.garanti.cepsubesi", "Kart işlemi", "1.250 TL card transaction", NotificationCategory.FINANCE),
            f("Bank transfer", "com.example.wallet", "Transfer", "Bank transfer completed", NotificationCategory.FINANCE),
            f("Balance update", "com.example.wallet", "Balance", "Your balance was updated", NotificationCategory.FINANCE),
            f("Trendyol cargo", "com.trendyol.android", "Siparişiniz", "Kargonuz dağıtıma çıktı", NotificationCategory.DELIVERY),
            f("Amazon shipped", "com.amazon.mShop.android.shopping", "Your order", "Your order has shipped", NotificationCategory.DELIVERY),
            f("Out for delivery", "com.example.store", "Package", "Out for delivery", NotificationCategory.DELIVERY),
            f("Delivered package", "com.example.store", "Package", "Your package was delivered", NotificationCategory.DELIVERY),
            f("Courier approaching", "com.example.food", "Order", "Courier is approaching", NotificationCategory.DELIVERY),
            f("Tracking number", "com.example.post", "Shipment", "Tracking number 123", NotificationCategory.DELIVERY),
            f("Trendyol promotion", "com.trendyol.android", "Sana özel fırsat", "Sepette yüzde 50 indirim", NotificationCategory.PROMOTION),
            f("Bank promotion", "com.akbank.android.apps.akbank_direkt", "Kampanya", "Sana özel indirim fırsatı", NotificationCategory.PROMOTION),
            f("Instagram promotion", "com.instagram.android", "Special offer", "Limited stock, buy now", NotificationCategory.PROMOTION),
            f("English sale", "com.example.shop", "Weekend sale", "Save now with this coupon", NotificationCategory.PROMOTION),
            f("Turkish coupon", "com.example.shop", "Kupon", "Kupon ile indirim", NotificationCategory.PROMOTION),
            f("English deal", "com.example.shop", "Deal", "Special price offer", NotificationCategory.PROMOTION),
            f("WhatsApp meeting", "com.whatsapp", "Ali", "Sana yazdı: Toplantı tamamlandı", NotificationCategory.MESSAGING),
            f("Telegram message", "org.telegram.messenger", "Ayşe", "New message received", NotificationCategory.MESSAGING),
            f("Signal message", "org.thoughtcrime.securesms", "Mehmet", "Sent you a message", NotificationCategory.MESSAGING),
            f("Missed call", "com.example.voip", "Call", "Missed call", NotificationCategory.MESSAGING),
            f("Android messages", "com.google.android.apps.messaging", "SMS", "Yeni mesaj", NotificationCategory.MESSAGING),
            f("Calendar meeting", "com.google.android.calendar", "Hatırlatıcı", "Proje toplantısı 10 dakika sonra başlıyor", NotificationCategory.REMINDER),
            f("English reminder", "com.google.android.calendar", "Reminder", "Meeting starts in 10 minutes", NotificationCategory.REMINDER),
            f("Appointment", "com.example.health", "Appointment", "Appointment tomorrow", NotificationCategory.REMINDER),
            f("Due today", "com.example.tasks", "Task reminder", "Report due today", NotificationCategory.REMINDER),
            f("Instagram like", "com.instagram.android", "Activity", "Ali liked your photo", NotificationCategory.SOCIAL),
            f("Facebook follower", "com.facebook.katana", "Social", "New follower", NotificationCategory.SOCIAL),
            f("Community mention", "com.example.community", "Mention", "Ayşe mentioned you", NotificationCategory.SOCIAL),
            f("System update", "com.android.systemui", "Update", "Software update available", NotificationCategory.SYSTEM),
            f("Battery low", "com.android.systemui", "Battery", "Battery low", NotificationCategory.SYSTEM),
            f("Permission required", "com.android.settings", "Permission", "Permission required", NotificationCategory.SYSTEM),
            f("Wholesale is not sale", "com.example.shop", "Wholesale report", "Monthly wholesale report", NotificationCategory.OTHER),
            f("WhatsApp clone", "com.example.whatsappclone", "Hello", "General information", NotificationCategory.OTHER),
            f("Generic note", "com.example.notes", "Note", "Your note was saved", NotificationCategory.OTHER),
        )
    }
}
