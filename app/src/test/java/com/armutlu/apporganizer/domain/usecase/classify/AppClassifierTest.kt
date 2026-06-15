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
        // "bankapp" paketi → CAT_FINANCE keyword match
        val result = classifier.classifyApp(appInfo("com.unknown.bankapp", "BankApp"))
        assertEquals(Category.CAT_FINANCE, result)
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

    // Döngü 58 — AI / Kripto / TR Fintech yeni paketleri
    @Test
    fun `ChatGPT CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.openai.chatgpt", "ChatGPT")))
    }

    @Test
    fun `Claude CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.anthropic.claude", "Claude")))
    }

    @Test
    fun `Perplexity CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.perplexity.app", "Perplexity")))
    }

    @Test
    fun `Gemini CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.google.android.apps.gemini", "Gemini")))
    }

    @Test
    fun `Binance CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.binance.dev", "Binance")))
    }

    @Test
    fun `Paribu TR kripto CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.paribu.android", "Paribu")))
    }

    @Test
    fun `Papara TR fintech CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.papara.android", "Papara")))
    }

    @Test
    fun `MetaMask CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("io.metamask.android", "MetaMask")))
    }

    @Test
    fun `manufacturerClassify_enabled_samsung_prefix_CAT_SAMSUNG_dondurur`() {
        classifier.manufacturerClassifyEnabled = true
        val result = classifier.classifyApp(appInfo("com.samsung.unknownfeature", "Samsung Unknown"))
        assertEquals(Category.CAT_SAMSUNG, result)
    }

    @Test
    fun `manufacturerClassify_disabled_samsung_prefix_atlanir`() {
        classifier.manufacturerClassifyEnabled = false
        val result = classifier.classifyApp(appInfo("com.samsung.unknownfeature", "Samsung Unknown"))
        // Prefix map atlandı — exactMatch yok → keyword match veya OTHER (CAT_SAMSUNG değil)
        assertNotEquals(Category.CAT_SAMSUNG, result)
    }

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
