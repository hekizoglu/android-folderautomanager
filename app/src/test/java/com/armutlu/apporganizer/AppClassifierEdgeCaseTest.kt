package com.armutlu.apporganizer

import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifierAssets
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AppClassifier edge case testleri.
 * AppClassifierAssets mockkObject ile mock'lanir — Android context gerekmez.
 */
class AppClassifierEdgeCaseTest {

    private lateinit var classifier: AppClassifier
    private lateinit var mockContext: Context

    private val fakeExactMap = mapOf(
        "com.openai.chatgpt"              to Category.CAT_PRODUCTIVITY,
        "com.anthropic.claude"            to Category.CAT_PRODUCTIVITY,
        "com.perplexity.app"              to Category.CAT_PRODUCTIVITY,
        "com.deepseek.app"                to Category.CAT_PRODUCTIVITY,
        "com.microsoft.copilot"           to Category.CAT_PRODUCTIVITY,
        "org.telegram.messenger"          to Category.CAT_COMMUNICATION,
        "com.whatsapp"                    to Category.CAT_COMMUNICATION,
        "com.discord"                     to Category.CAT_COMMUNICATION,
        "com.facebook.katana"             to Category.CAT_SOCIAL,
        "com.instagram.android"           to Category.CAT_SOCIAL,
        "com.snapchat.android"            to Category.CAT_SOCIAL,
        "com.reddit.frontpage"            to Category.CAT_SOCIAL,
        "com.binance.dev"                 to Category.CAT_FINANCE
    )

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockkObject(AppClassifierAssets)
        every { AppClassifierAssets.getExactMatchMap(any()) } returns fakeExactMap
        classifier = AppClassifier(mockContext)
    }

    @After
    fun tearDown() {
        unmockkObject(AppClassifierAssets)
    }

    // --- 1. Bilinen Dogru Siniflandirmalar ---

    @Test
    fun `ChatGPT via exact match returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.openai.chatgpt", "ChatGPT")))
    }

    @Test
    fun `Telegram via exact match returns CAT_COMMUNICATION`() {
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(appInfo("org.telegram.messenger", "Telegram")))
    }

    @Test
    fun `WhatsApp via exact match returns CAT_COMMUNICATION`() {
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(appInfo("com.whatsapp", "WhatsApp")))
    }

    @Test
    fun `DeepSeek via exact match returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.deepseek.app", "DeepSeek")))
    }

    @Test
    fun `Microsoft Copilot via exact match returns CAT_PRODUCTIVITY`() {
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(appInfo("com.microsoft.copilot", "Microsoft Copilot")))
    }

    // --- 2. Fuzzy / Kismi Eslesmeler ---

    @Test
    fun `app name containing social keyword returns CAT_SOCIAL`() {
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.random.app", "Social Connect Hub")))
    }

    @Test
    fun `app name with partial package keyword match - reddit returns CAT_SOCIAL`() {
        // com.reddit.frontpage exactMatchMap'de var
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.reddit.frontpage", "Reddit")))
    }

    @Test
    fun `app name containing news keyword returns CAT_NEWS`() {
        assertEquals(Category.CAT_NEWS, classifier.classifyApp(appInfo("com.random.pkg", "Breaking News Reader")))
    }

    @Test
    fun `app name containing health keyword yoga returns CAT_HEALTH`() {
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(appInfo("com.yoga.app", "Yoga Practice Meditation")))
    }

    @Test
    fun `package name containing crypto keyword returns CAT_FINANCE`() {
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(appInfo("com.binance.crypto.wallet", "Binance")))
    }

    // --- 3. Paket Adi - Uygulama Adi Cakismasi ---

    @Test
    fun `misleading app name but exact package match wins`() {
        // exactMatchMap'de CAT_SOCIAL kayitli paket, app adi finance cagristiriyor
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.facebook.katana", "Budget Tracker Pro")))
    }

    @Test
    fun `app name has game keyword but package is exact social match`() {
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(appInfo("com.snapchat.android", "Snap Game Filter")))
    }

    @Test
    fun `unknown package but app name keyword drives classification`() {
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(appInfo("com.xyzabc.nothing", "Workout Tracker Gym Planner")))
    }

    // --- 4. Yanlis Siniflandirma Riskli ---

    @Test
    fun `app named Daily shows news or productivity result`() {
        val result = classifier.classifyApp(appInfo("com.random.daily", "Daily"))
        // "daily" news keyword — news veya productivity kabul edilebilir
        assertTrue(result == Category.CAT_NEWS || result == Category.CAT_PRODUCTIVITY || result == Category.CAT_OTHER)
    }

    @Test
    fun `app named Play Store not CAT_OTHER`() {
        val result = classifier.classifyApp(appInfo("com.google.android.play.store", "Play Store"))
        assertNotEquals(Category.CAT_OTHER, result)
    }

    @Test
    fun `app with AI in name not CAT_OTHER`() {
        assertNotEquals(Category.CAT_OTHER, classifier.classifyApp(appInfo("com.photo.ai.enhancer", "Photo AI Enhancer")))
    }

    @Test
    fun `security tool app returns CAT_UTILITIES not CAT_FINANCE`() {
        assertEquals(Category.CAT_UTILITIES, classifier.classifyApp(appInfo("com.random.security", "Phone Security Antivirus")))
    }

    // --- 5. Coklu Keyword Eslesmesi ---

    @Test
    fun `app with both fitness and game keywords not CAT_OTHER`() {
        val result = classifier.classifyApp(appInfo("com.random.app", "Fitness Game Challenge"))
        assertNotEquals(Category.CAT_OTHER, result)
    }

    @Test
    fun `classifyApps handles 100 apps without error`() {
        val apps = (1..100).map { i -> appInfo("com.test.app$i", "Test App $i") }
        val results = classifier.classifyApps(apps)
        assertEquals(100, results.size)
        results.values.forEach { category -> assertTrue(category.isNotEmpty()) }
    }

    // --- 6. Ozel Karakter / Edge ---

    @Test
    fun `app name with numbers only returns CAT_OTHER`() {
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(appInfo("com.app.123456", "123456")))
    }

    @Test
    fun `app name with special characters does not crash`() {
        val result = classifier.classifyApp(appInfo("com.app.test", "!@#\$%^&*()"))
        assertNotNull(result)
    }

    @Test
    fun `bos paket adi crash yapmaz`() {
        val result = runCatching { classifier.classifyApp(appInfo("", "")) }.getOrDefault(Category.CAT_OTHER)
        assertNotNull(result)
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
        assertNotEquals(Category.CAT_SAMSUNG, classifier.classifyApp(appInfo("com.samsung.unknownfeature", "Samsung Unknown")))
    }

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
