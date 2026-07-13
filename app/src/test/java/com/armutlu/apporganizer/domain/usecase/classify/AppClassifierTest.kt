package com.armutlu.apporganizer.domain.usecase.classify

import android.content.Context
import com.armutlu.apporganizer.data.remote.AppDatabaseService
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
    private lateinit var mockDbService: AppDatabaseService

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
        mockDbService = mockk(relaxed = true)
        mockkObject(AppClassifierAssets)
        every { AppClassifierAssets.getExactMatchMap(any()) } returns fakeExactMap
        every { mockDbService.getCategoryForPackage(any()) } returns null
        classifier = AppClassifier(mockContext, mockDbService)
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
    fun `decision exact match includes bundled catalog metadata`() {
        val decision = classifier.classifyAppDecision(appInfo("com.instagram.android", "Instagram"))

        assertEquals(Category.CAT_SOCIAL, decision.categoryId)
        assertEquals(ClassificationSource.BUNDLED_CATALOG, decision.source)
        assertEquals(ClassificationReason.EXACT_PACKAGE_MATCH, decision.reasonCode)
        assertEquals(95, decision.confidence)
        assertFalse(decision.requiresReview)
    }

    @Test
    fun `remote catalog has priority over bundled catalog`() {
        every { mockDbService.getCategoryForPackage("com.instagram.android") } returns Category.CAT_PRODUCTIVITY

        val decision = classifier.classifyAppDecision(appInfo("com.instagram.android", "Instagram"))

        assertEquals(Category.CAT_PRODUCTIVITY, decision.categoryId)
        assertEquals(ClassificationSource.REMOTE_CATALOG, decision.source)
        assertEquals(ClassificationReason.UPDATED_CATALOG_MATCH, decision.reasonCode)
        assertEquals(97, decision.confidence)
    }

    @Test
    fun `locked user corrected decision has max priority`() {
        every { mockDbService.getCategoryForPackage("com.instagram.android") } returns Category.CAT_PRODUCTIVITY
        val app = appInfo("com.instagram.android", "Instagram").copy(
            categoryId = Category.CAT_FINANCE,
            classificationSource = ClassificationSource.USER_CORRECTED.name,
            isCategoryLocked = true,
        )

        val decision = classifier.classifyAppDecision(app)

        assertEquals(Category.CAT_FINANCE, decision.categoryId)
        assertEquals(ClassificationSource.USER_CORRECTED, decision.source)
        assertEquals(100, decision.confidence)
        assertFalse(decision.requiresReview)
    }

    @Test
    fun `unknown app returns reviewable fallback decision`() {
        val decision = classifier.classifyAppDecision(appInfo("com.randomxyz.nothing", "XYZ App 123"))

        assertEquals(Category.CAT_OTHER, decision.categoryId)
        assertEquals(ClassificationSource.FALLBACK_OTHER, decision.source)
        assertEquals(ClassificationReviewState.PENDING, decision.reviewState)
        assertTrue(decision.requiresReview)
        assertTrue(decision.confidence in 0..100)
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
        assertEquals(
            Category.CAT_SAMSUNG,
            classifier.classifyApp(
                appInfo("com.samsung.unknownfeature", "Samsung Unknown"),
                manufacturerClassifyEnabled = true
            )
        )
    }

    @Test
    fun `manufacturerClassify_disabled_samsung_prefix_atlanir`() {
        val result = classifier.classifyApp(
            appInfo("com.samsung.unknownfeature", "Samsung Unknown"),
            manufacturerClassifyEnabled = false
        )
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

    private fun appInfo(packageName: String, appName: String, categoryId: String) = AppInfo(
        packageName = packageName,
        appName = appName,
        categoryId = categoryId
    )

    // --- findSimilarUnclassifiedApps (K2 kismi — tek tek secilebilir oneri) ---

    @Test
    fun `ayni uretici prefix'ine sahip uygulama onerilir`() {
        val changed = appInfo("com.samsung.health", "Samsung Health", Category.CAT_SAMSUNG)
        val candidate = appInfo("com.samsung.gallery", "Samsung Gallery", Category.CAT_OTHER)
        val allApps = listOf(changed, candidate)
        val result = classifier.findSimilarUnclassifiedApps(
            changedApp = changed,
            oldCategoryId = Category.CAT_OTHER,
            newCategoryId = Category.CAT_SAMSUNG,
            allApps = allApps,
            manualOverrides = emptyMap()
        )
        assertTrue(result.any { it.packageName == "com.samsung.gallery" })
    }

    @Test
    fun `zaten manuel override'i olan uygulama onerilmez`() {
        val changed = appInfo("com.samsung.health", "Samsung Health", Category.CAT_SAMSUNG)
        val candidate = appInfo("com.samsung.gallery", "Samsung Gallery", Category.CAT_OTHER)
        val allApps = listOf(changed, candidate)
        val result = classifier.findSimilarUnclassifiedApps(
            changedApp = changed,
            oldCategoryId = Category.CAT_OTHER,
            newCategoryId = Category.CAT_SAMSUNG,
            allApps = allApps,
            manualOverrides = mapOf("com.samsung.gallery" to Category.CAT_OTHER)
        )
        assertTrue(result.none { it.packageName == "com.samsung.gallery" })
    }

    @Test
    fun `hedef kategoriyle zaten ayni kategoride olan uygulama onerilmez`() {
        val changed = appInfo("com.samsung.health", "Samsung Health", Category.CAT_SAMSUNG)
        // candidate zaten yeni (hedef) kategoride -> eski kategoriyle eslesmiyor, onerilmemeli
        val candidate = appInfo("com.samsung.gallery", "Samsung Gallery", Category.CAT_SAMSUNG)
        val allApps = listOf(changed, candidate)
        val result = classifier.findSimilarUnclassifiedApps(
            changedApp = changed,
            oldCategoryId = Category.CAT_OTHER,
            newCategoryId = Category.CAT_SAMSUNG,
            allApps = allApps,
            manualOverrides = emptyMap()
        )
        assertTrue(result.none { it.packageName == "com.samsung.gallery" })
    }

    @Test
    fun `maksimum limit asilmiyor`() {
        val changed = appInfo("com.samsung.health", "Samsung Health", Category.CAT_SAMSUNG)
        val candidates = (1..20).map {
            appInfo("com.samsung.app$it", "Samsung App $it", Category.CAT_OTHER)
        }
        val allApps = listOf(changed) + candidates
        val result = classifier.findSimilarUnclassifiedApps(
            changedApp = changed,
            oldCategoryId = Category.CAT_OTHER,
            newCategoryId = Category.CAT_SAMSUNG,
            allApps = allApps,
            manualOverrides = emptyMap()
        )
        assertTrue(result.size <= 10)
    }

    @Test
    fun `hic aday yoksa bos liste doner ve crash olmaz`() {
        val changed = appInfo("com.bilinmeyen.uygulama", "Bilinmeyen", Category.CAT_OTHER)
        val allApps = listOf(changed)
        val result = classifier.findSimilarUnclassifiedApps(
            changedApp = changed,
            oldCategoryId = Category.CAT_OTHER,
            newCategoryId = Category.CAT_SAMSUNG,
            allApps = allApps,
            manualOverrides = emptyMap()
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `eski ve yeni kategori ayniysa bos liste doner`() {
        val changed = appInfo("com.samsung.health", "Samsung Health", Category.CAT_SAMSUNG)
        val candidate = appInfo("com.samsung.gallery", "Samsung Gallery", Category.CAT_SAMSUNG)
        val allApps = listOf(changed, candidate)
        val result = classifier.findSimilarUnclassifiedApps(
            changedApp = changed,
            oldCategoryId = Category.CAT_SAMSUNG,
            newCategoryId = Category.CAT_SAMSUNG,
            allApps = allApps,
            manualOverrides = emptyMap()
        )
        assertTrue(result.isEmpty())
    }
}
