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
            val result = classifier.classify("fixture-$index", fixture.pkg, fixture.title, fixture.text, index.toLong(), fixture.priority)
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
    fun `low value categories are suppressible while direct communication is not`() {
        val promotion = classifier.classify("promo", "com.trendyol.android", "Sana özel fırsat", "Siparişine özel kupon ve yüzde 50 indirim", 1L)
        val news = classifier.classify("news", "com.example.news", "Haber", "Bülten ve son dakika gelişmeleri", 2L)
        val media = classifier.classify("media", "com.google.android.youtube", "Recommended", "Recommended video for you", 3L)
        val message = classifier.classify("message", "com.whatsapp", "Ali", "Sana yazdı", 4L)

        assertEquals(NotificationCategory.PROMOTION, promotion.category)
        assertTrue(promotion.shouldSuppress)
        assertEquals(NotificationCategory.NEWS, news.category)
        assertEquals(NotificationCategory.MEDIA, media.category)
        assertFalse(message.shouldSuppress)
    }

    @Test
    fun `specialized categories cover real world notification gaps`() {
        val missedCall = classifier.classify("call", "com.google.android.dialer", "Telefon", "Cevapsız arama", 1L)
        val calendar = classifier.classify("calendar", "com.google.android.calendar", "Mesai", "Mesai 10 dakika sonra başlıyor", 2L)
        val market = classifier.classify("market", "com.google.android.googlequicksearchbox", "HPE", "HPE %3,9 arttı", 3L)
        val family = classifier.classify("family", "com.google.android.apps.kids", "Family Link", "Yeni uygulama yüklendi", 4L)

        assertEquals(NotificationCategory.MISSED_CALL, missedCall.category)
        assertEquals(NotificationCategory.CALENDAR, calendar.category)
        assertTrue(calendar.importanceScore >= 70)
        assertEquals(NotificationCategory.MARKET, market.category)
        assertEquals(NotificationCategory.FAMILY, family.category)
    }

    @Test
    fun `messaging package wins over generic reminder words`() {
        val result = classifier.classify(
            "whatsapp-meeting",
            "com.whatsapp",
            "Ali",
            "Sana yazdı: toplantı yarın saat 10'da",
            1L,
        )
        assertEquals(NotificationCategory.MESSAGING, result.category)
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
            f("Card transaction", "com.garanti.cepsubesi", "Kart işlemi", "1.250 TL card transaction", NotificationCategory.FINANCE),
            f("Bank transfer", "com.example.wallet", "Transfer", "Bank transfer completed", NotificationCategory.FINANCE),
            f("Trendyol cargo", "com.trendyol.android", "Siparişiniz", "Kargonuz dağıtıma çıktı", NotificationCategory.DELIVERY),
            f("Amazon shipped", "com.amazon.mShop.android.shopping", "Your order", "Your order has shipped", NotificationCategory.DELIVERY),
            f("Trendyol promotion", "com.trendyol.android", "Sana özel fırsat", "Siparişine özel kupon ve yüzde 50 indirim", NotificationCategory.PROMOTION),
            f("WhatsApp meeting", "com.whatsapp", "Ali", "Sana yazdı: Toplantı tamamlandı", NotificationCategory.MESSAGING),
            f("Telegram message", "org.telegram.messenger", "Ayşe", "New message received", NotificationCategory.MESSAGING),
            f("Missed call", "com.google.android.dialer", "Call", "Missed call", NotificationCategory.MISSED_CALL),
            f("Calendar meeting", "com.google.android.calendar", "Hatırlatıcı", "Proje toplantısı 10 dakika sonra başlıyor", NotificationCategory.CALENDAR),
            f("English reminder", "com.google.android.calendar", "Reminder", "Meeting starts in 10 minutes", NotificationCategory.CALENDAR),
            f("Instagram like", "com.instagram.android", "Activity", "Ali liked your photo", NotificationCategory.SOCIAL),
            f("Market alert", "com.google.android.googlequicksearchbox", "HPE", "HPE %3,9 arttı", NotificationCategory.MARKET),
            f("Family Link", "com.google.android.apps.kids", "Family Link", "Yeni uygulama yüklendi", NotificationCategory.FAMILY),
            f("News", "com.example.news", "Haber", "Bülten ve son dakika gelişmeleri", NotificationCategory.NEWS),
            f("Media", "com.google.android.youtube", "Recommended", "Recommended video for you", NotificationCategory.MEDIA),
            f("System update", "com.android.systemui", "Update", "Software update available", NotificationCategory.UPDATE),
            f("Battery low", "com.android.systemui", "Battery", "Battery low", NotificationCategory.SYSTEM),
            f("Generic note", "com.example.notes", "Note", "Your note was saved", NotificationCategory.OTHER),
        )
    }
}
