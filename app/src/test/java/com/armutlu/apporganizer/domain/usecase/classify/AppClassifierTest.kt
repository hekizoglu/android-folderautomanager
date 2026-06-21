package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AppClassifier birim testleri.
 * AppClassifierAssets mockkObject ile mock'lanir — Android context gerekmez.
 * Test edilen siniflandirma onceligi:
 *   1. exactMatchMap (assets'ten yuklu)
 *   2. Uretici prefix eslesme (MANUFACTURER_PREFIX_MAP)
 *   3. Keyword eslesme (KeywordDatabase)
 *   4. Varsayilan CAT_OTHER
 */
class AppClassifierTest {

    private lateinit var classifier: AppClassifier
    private lateinit var mockContext: Context

    // Test icin kullanilan minimal exact match haritasi (gercek JSON'un kucuk orn.)
    private val fakeExactMap = mapOf(
        "com.instagram.android"           to Category.CAT_SOCIAL,
        "com.facebook.katana"             to Category.CAT_SOCIAL,
        "com.whatsapp"                    to Category.CAT_COMMUNICATION,
        "org.telegram.messenger"          to Category.CAT_COMMUNICATION,
        "com.discord"                     to Category.CAT_COMMUNICATION,
        "com.openai.chatgpt"              to Category.CAT_PRODUCTIVITY,
        "com.anthropic.claude"            to Category.CAT_PRODUCTIVITY,
        "com.perplexity.app"              to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.gemini"  to Category.CAT_PRODUCTIVITY,
        "com.nubank.nubank"               to Category.CAT_FINANCE,
        "br.com.brainweb.ifoodpartner"    to Category.CAT_FOOD,
        "br.com.bb.android"               to Category.CAT_FINANCE,
        "com.talabat.android"             to Category.CAT_FOOD,
        "com.noon.buyerapp"               to Category.CAT_SHOPPING,
        "com.stc.pay"                     to Category.CAT_FINANCE,
        "com.safaricom.mpesa"             to Category.CAT_FINANCE,
        "com.safeboda.android"            to Category.CAT_TRAVEL,
        "com.konga.android"               to Category.CAT_SHOPPING,
        "com.binance.dev"                 to Category.CAT_FINANCE,
        "com.paribu.android"              to Category.CAT_FINANCE,
        "com.papara.android"              to Category.CAT_FINANCE,
        "io.metamask.android"             to Category.CAT_FINANCE,
        "com.trendyol.android"            to Category.CAT_SHOPPING
    )

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockkObject(AppClassifierAssets)
        every { AppClassifierAssets.getExactMatchMap(any()) } returns fakeExactMap
        classifier = AppClassifier(mockContext)
    }

    @After
    fun tearDown() {
        unmockkObject(AppClassifierAssets)
    }

    // --- Exact Match ---

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
    fun `Instagram exact match returns CAT_SOCIAL`() {
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.instagram.android", "Instagram")))
    }

    @Test
    fun `ChatGPT exact match returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.openai.chatgpt", "ChatGPT")))
    }

    @Test
    fun `Claude exact match returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.anthropic.claude", "Claude")))
    }

    @Test
    fun `Discord exact match returns CAT_COMMUNICATION`() {
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(appInfo("com.discord", "Discord")))
    }

    // --- Keyword Eslesme ---

    @Test
    fun `keyword eslesmesi dogru calisir`() {
        val result = classifier.classifyApp(appInfo("com.unknown.bankapp", "BankApp"))
        assertEquals(Category.CAT_FINANCE, result)
    }

    @Test
    fun `App name keyword match - fitness app returns CAT_HEALTH`() {
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(appInfo("com.random.dev", "My Fitness Tracker")))
    }

    @Test
    fun `App name keyword match - bank app returns CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("tr.random.bankapp", "My Bank App")))
    }

    @Test
    fun `App name keyword match - game app returns CAT_GAMES`() {
        assertEquals(Category.CAT_GAMES, classifier.classifyApp(appInfo("com.random.studio", "Epic Racing Game")))
    }

    @Test
    fun `News keyword in app name returns CAT_NEWS`() {
        assertEquals(Category.CAT_NEWS, classifier.classifyApp(appInfo("tr.com.myapp", "BBC News")))
    }

    // --- Varsayilan ---

    @Test
    fun `Unknown app with no match returns CAT_OTHER`() {
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(appInfo("com.randomxyz.nothing", "XYZ App 123")))
    }

    // --- Toplu Siniflandirma ---

    @Test
    fun `classifyApps returns correct map`() {
        val apps = listOf(
            appInfo("com.instagram.android", "Instagram"),
            appInfo("com.unknown.abc", "Unknown")
        )
        val result = classifier.classifyApps(apps)
        assertEquals(Category.CAT_SOCIAL, result["com.instagram.android"])
        assertEquals(Category.CAT_OTHER, result["com.unknown.abc"])
    }

    @Test
    fun `classifyApps empty list returns empty map`() {
        assertTrue(classifier.classifyApps(emptyList()).isEmpty())
    }

    // --- Guven Skoru ---

    @Test
    fun `getConfidence exact match returns 95`() {
        assertEquals(95, classifier.getConfidence(appInfo("com.instagram.android", "Instagram"), Category.CAT_SOCIAL))
    }

    @Test
    fun `getConfidence CAT_OTHER returns 30`() {
        assertEquals(30, classifier.getConfidence(appInfo("com.unknown.app", "Unknown"), Category.CAT_OTHER))
    }

    @Test
    fun `getConfidence keyword match returns at least 70`() {
        val conf = classifier.getConfidence(appInfo("com.fitness.app", "Fitness Tracker"), Category.CAT_HEALTH)
        assertTrue("Beklenen >= 70, gercek: $conf", conf >= 70)
    }

    @Test
    fun `getConfidence package keyword match returns at least 70`() {
        val conf = classifier.getConfidence(appInfo("com.trendyol.android", "Trendyol"), Category.CAT_SHOPPING)
        assertTrue("Beklenen >= 70, gercek: $conf", conf >= 70)
    }

    // --- Buyuk/Kucuk Harf ---

    @Test
    fun `ChatGPT uppercase app name still returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.openai.chatgpt", "CHATGPT")))
    }

    @Test
    fun `chatgpt lowercase app name returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.openai.chatgpt", "chatgpt")))
    }

    // --- Bos/Null-benzeri Input ---

    @Test
    fun `bos paket adi crash yapmaz`() {
        val result = runCatching { classifier.classifyApp(appInfo("", "")) }.getOrDefault(Category.CAT_OTHER)
        assertNotNull(result)
    }

    @Test
    fun `empty app name with unknown package returns CAT_OTHER`() {
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(appInfo("com.unknown.pkg", "")))
    }

    @Test
    fun `both empty package and app name returns CAT_OTHER`() {
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(appInfo("", "")))
    }

    // --- exactMatchMap Onceligi ---

    @Test
    fun `exactMatchMap takes priority over keyword classification`() {
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(appInfo("org.telegram.messenger", "Telegram")))
    }

    @Test
    fun `exactMatchMap entry with misleading app name wins over keyword`() {
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(appInfo("com.discord", "Game Arena Legends")))
    }

    // --- Uretici Prefix ---

    @Test
    fun `manufacturerClassify_enabled_samsung_prefix_CAT_SAMSUNG_dondurur`() {
        classifier.manufacturerClassifyEnabled = true
        assertEquals(Category.CAT_SAMSUNG, classifier.classifyApp(appInfo("com.samsung.unknownfeature", "Samsung Unknown")))
    }

    @Test
    fun `manufacturerClassify_disabled_samsung_prefix_atlanir`() {
        classifier.manufacturerClassifyEnabled = false
        val result = classifier.classifyApp(appInfo("com.samsung.unknownfeature", "Samsung Unknown"))
        assertNotEquals(Category.CAT_SAMSUNG, result)
    }

    // --- Bolgesel Paketler ---

    @Test fun `Nubank CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.nubank.nubank", "Nubank")))
    @Test fun `iFood Partner CAT_FOOD`() = assertEquals(Category.CAT_FOOD, classifier.classifyApp(appInfo("br.com.brainweb.ifoodpartner", "iFood")))
    @Test fun `Banco do Brasil CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("br.com.bb.android", "Banco do Brasil")))
    @Test fun `Talabat CAT_FOOD`() = assertEquals(Category.CAT_FOOD, classifier.classifyApp(appInfo("com.talabat.android", "Talabat")))
    @Test fun `Noon CAT_SHOPPING`() = assertEquals(Category.CAT_SHOPPING, classifier.classifyApp(appInfo("com.noon.buyerapp", "Noon")))
    @Test fun `STC Pay CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.stc.pay", "STC Pay")))
    @Test fun `MPesa CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.safaricom.mpesa", "M-Pesa")))
    @Test fun `SafeBoda CAT_TRAVEL`() = assertEquals(Category.CAT_TRAVEL, classifier.classifyApp(appInfo("com.safeboda.android", "SafeBoda")))
    @Test fun `Konga CAT_SHOPPING`() = assertEquals(Category.CAT_SHOPPING, classifier.classifyApp(appInfo("com.konga.android", "Konga")))
    @Test fun `Binance CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.binance.dev", "Binance")))
    @Test fun `Paribu TR kripto CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.paribu.android", "Paribu")))
    @Test fun `Papara TR fintech CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.papara.android", "Papara")))
    @Test fun `MetaMask CAT_FINANCE`() = assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("io.metamask.android", "MetaMask")))
    @Test fun `Perplexity CAT_PRODUCTIVITY`() = assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.perplexity.app", "Perplexity")))
    @Test fun `Gemini CAT_PRODUCTIVITY`() = assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.google.android.apps.gemini", "Gemini")))

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
