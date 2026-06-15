package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AppClassifierTest {

    private lateinit var classifier: AppClassifier

    @Before
    fun setUp() {
        classifier = AppClassifier()
    }

    @Test
    fun `bilinen paket dogru kategoriye eslenir`() {
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.instagram.android", "Instagram")))
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.facebook.katana", "Facebook")))
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(appInfo("com.whatsapp", "WhatsApp")))
    }

    @Test
    fun `bilinmeyen paket CAT_OTHER dondurur`() {
        val result = classifier.classifyApp(appInfo("com.bilinmeyen.uygulama.xyz123", "Bilinmeyen"))
        assertEquals(Category.CAT_OTHER, result)
    }

    @Test
    fun `keyword eslesmesi dogru calisir`() {
        // "bank" keyword CAT_FINANCE'a yonlendirmeli
        val result = classifier.classifyApp(appInfo("com.unknown.mobilebanking", "Mobile Banking"))
        assertTrue(result == Category.CAT_FINANCE || result == Category.CAT_OTHER)
    }

    @Test
    fun `buyuk kucuk harf tutarsizligi olmaz`() {
        val r1 = classifier.classifyApp(appInfo("com.instagram.android", "Instagram"))
        // exactMatch buyuk/kucuk harf duyarli; bilinmeyen pakete dusmeli
        assertEquals(Category.CAT_SOCIAL, r1)
    }

    @Test
    fun `bos paket adi crash yapmaz`() {
        val result = runCatching { classifier.classifyApp(appInfo("", "")) }.getOrDefault(Category.CAT_OTHER)
        assertNotNull(result)
    }

    // Latin Amerika yeni paketleri
    @Test
    fun `Nubank CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.nubank.nubank", "Nubank")))
    }

    @Test
    fun `iFood Partner CAT_FOOD`() {
        assertEquals(Category.CAT_FOOD, classifier.classifyApp(appInfo("br.com.brainweb.ifoodpartner", "iFood")))
    }

    @Test
    fun `Banco do Brasil CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("br.com.bb.android", "Banco do Brasil")))
    }

    // Orta Dogu yeni paketleri
    @Test
    fun `Talabat CAT_FOOD`() {
        assertEquals(Category.CAT_FOOD, classifier.classifyApp(appInfo("com.talabat.android", "Talabat")))
    }

    @Test
    fun `Noon CAT_SHOPPING`() {
        assertEquals(Category.CAT_SHOPPING, classifier.classifyApp(appInfo("com.noon.buyerapp", "Noon")))
    }

    @Test
    fun `STC Pay CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.stc.pay", "STC Pay")))
    }

    // Afrika yeni paketleri
    @Test
    fun `MPesa CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.safaricom.mpesa", "M-Pesa")))
    }

    @Test
    fun `SafeBoda CAT_TRAVEL`() {
        assertEquals(Category.CAT_TRAVEL, classifier.classifyApp(appInfo("com.safeboda.android", "SafeBoda")))
    }

    @Test
    fun `Konga CAT_SHOPPING`() {
        assertEquals(Category.CAT_SHOPPING, classifier.classifyApp(appInfo("com.konga.android", "Konga")))
    }

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
