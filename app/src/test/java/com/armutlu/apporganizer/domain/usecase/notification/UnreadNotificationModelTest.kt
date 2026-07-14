package com.armutlu.apporganizer.domain.usecase.notification

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P0.5 — UnreadNotificationModel saf fonksiyon testleri.
 * Senaryolar: yeni bildirim -> okunmamis, launchApp sonrasi -> sifirlanir,
 * okuduktan sonra yeni bildirim -> tekrar artar, lastReadAt hic yoksa davranis.
 */
class UnreadNotificationModelTest {

    @Test
    fun `yeni bildirim gelince ve hic acilmamissa okunmamis sayilir`() {
        // lastReadAt yok (null) -> guvenli varsayilan: aktif sayiyi goster
        val result = UnreadNotificationModel.unreadCountFor(
            activeCount = 3,
            lastPostedAt = 1000L,
            lastReadAt = null,
        )
        assertEquals(3, result)
    }

    @Test
    fun `uygulama bildirimden sonra acilinca badge sifirlanir`() {
        val result = UnreadNotificationModel.unreadCountFor(
            activeCount = 2,
            lastPostedAt = 1000L,
            lastReadAt = 2000L, // okuma, bildirimden sonra
        )
        assertEquals(0, result)
    }

    @Test
    fun `okunduktan sonra yeni bildirim gelince tekrar okunmamis olur`() {
        val result = UnreadNotificationModel.unreadCountFor(
            activeCount = 1,
            lastPostedAt = 5000L, // yeni bildirim, okumadan SONRA geldi
            lastReadAt = 2000L,
        )
        assertEquals(1, result)
    }

    @Test
    fun `lastPostedAt bilinmiyorsa guvenli varsayilan aktif sayiyi gosterir`() {
        val result = UnreadNotificationModel.unreadCountFor(
            activeCount = 4,
            lastPostedAt = null,
            lastReadAt = 9999L,
        )
        assertEquals(4, result)
    }

    @Test
    fun `aktif bildirim yoksa sonuc her zaman sifirdir`() {
        val result = UnreadNotificationModel.unreadCountFor(
            activeCount = 0,
            lastPostedAt = 1000L,
            lastReadAt = null,
        )
        assertEquals(0, result)
    }

    @Test
    fun `esit zaman damgasinda okunmus sayilir (sinir durum)`() {
        val result = UnreadNotificationModel.unreadCountFor(
            activeCount = 5,
            lastPostedAt = 1000L,
            lastReadAt = 1000L,
        )
        assertEquals(0, result)
    }

    @Test
    fun `computeUnreadCounts sadece okunmamis paketleri harita olarak dondurur`() {
        val active = mapOf("com.a" to 2, "com.b" to 1, "com.c" to 3)
        val posted = mapOf("com.a" to 5000L, "com.b" to 1000L, "com.c" to 8000L)
        val read = mapOf("com.a" to 1000L, "com.b" to 2000L) // com.a: okunmamis, com.b: okunmus, com.c: hic okunmadi

        val result = UnreadNotificationModel.computeUnreadCounts(active, posted, read)

        assertEquals(mapOf("com.a" to 2, "com.c" to 3), result)
    }

    @Test
    fun `computeUnreadCounts bos aktif haritada bos sonuc dondurur`() {
        val result = UnreadNotificationModel.computeUnreadCounts(emptyMap(), emptyMap(), emptyMap())
        assertEquals(emptyMap<String, Int>(), result)
    }
}
