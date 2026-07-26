package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationClassifierUseCaseTest {

    private val classifier = NotificationClassifierUseCase()

    @Test
    fun `bank security code is finance sensitive and high priority`() {
        val result = classifier.classify(
            key = "bank-1",
            packageName = "com.akbank.android.apps.akbank_direkt",
            title = "Güvenlik doğrulaması",
            text = "123456 tek kullanımlık doğrulama kodunuz",
            timestamp = 1L,
            systemPriority = 1,
        )

        assertEquals(NotificationCategory.FINANCE, result.category)
        assertTrue(result.isSensitive)
        assertTrue(result.importanceScore >= 80)
        assertFalse(result.shouldSuppress)
    }

    @Test
    fun `cargo status is classified as delivery`() {
        val result = classifier.classify(
            key = "cargo-1",
            packageName = "com.trendyol.android",
            title = "Siparişiniz",
            text = "Kargonuz dağıtıma çıktı",
            timestamp = 2L,
        )

        assertEquals(NotificationCategory.DELIVERY, result.category)
        assertFalse(result.shouldSuppress)
    }

    @Test
    fun `shopping app campaign remains promotion instead of delivery`() {
        val result = classifier.classify(
            key = "shopping-promo-1",
            packageName = "com.trendyol.android",
            title = "Sana özel fırsat",
            text = "Sepette yüzde 50 indirim",
            timestamp = 3L,
        )

        assertEquals(NotificationCategory.PROMOTION, result.category)
        assertTrue(result.shouldSuppress)
    }

    @Test
    fun `bank campaign remains promotion instead of finance`() {
        val result = classifier.classify(
            key = "bank-promo-1",
            packageName = "com.akbank.android.apps.akbank_direkt",
            title = "Kampanya",
            text = "Sana özel indirim fırsatı",
            timestamp = 4L,
        )

        assertEquals(NotificationCategory.PROMOTION, result.category)
        assertTrue(result.shouldSuppress)
    }

    @Test
    fun `discount campaign is suppressed promotion`() {
        val result = classifier.classify(
            key = "promo-1",
            packageName = "com.example.shopping",
            title = "Sana özel fırsat",
            text = "Sepette yüzde 50 indirim, hemen al",
            timestamp = 5L,
        )

        assertEquals(NotificationCategory.PROMOTION, result.category)
        assertTrue(result.shouldSuppress)
        assertTrue(result.importanceScore < 40)
    }

    @Test
    fun `whatsapp notification remains visible messaging`() {
        val result = classifier.classify(
            key = "message-1",
            packageName = "com.whatsapp",
            title = "Ali",
            text = "Sana yazdı: Toplantı tamamlandı",
            timestamp = 6L,
        )

        assertEquals(NotificationCategory.MESSAGING, result.category)
        assertFalse(result.shouldSuppress)
        assertTrue(result.importanceScore >= 60)
    }

    @Test
    fun `calendar meeting is reminder`() {
        val result = classifier.classify(
            key = "calendar-1",
            packageName = "com.google.android.calendar",
            title = "Hatırlatıcı",
            text = "Proje toplantısı 10 dakika sonra başlıyor",
            timestamp = 7L,
        )

        assertEquals(NotificationCategory.REMINDER, result.category)
        assertFalse(result.shouldSuppress)
    }
}
