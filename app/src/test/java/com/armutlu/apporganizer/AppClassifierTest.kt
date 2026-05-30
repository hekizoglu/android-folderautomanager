package com.armutlu.apporganizer

import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.AppClassifier
import com.armutlu.apporganizer.domain.usecase.KeywordDatabase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AppClassifier birim testleri.
 *
 * Sınıflandırma öncelik sırası:
 *   1. Online veritabanı (AppDatabaseService)
 *   2. Exact match (exactMatchMap)
 *   3. Keyword eşleşmesi (KeywordDatabase)
 *   4. Varsayılan → CAT_OTHER
 */
class AppClassifierTest {

    private lateinit var mockDbService: AppDatabaseService
    private lateinit var classifier: AppClassifier

    @Before
    fun setup() {
        mockDbService = mockk()
        every { mockDbService.getCategoryForPackage(any()) } returns null // varsayılan: online DB'de yok
        classifier = AppClassifier(mockDbService)
    }

    // ─── Exact Match ─────────────────────────────────────────────────────────

    @Test
    fun `Instagram exact match returns CAT_SOCIAL`() {
        val app = appInfo("com.instagram.android", "Instagram")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `ChatGPT exact match returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.openai.chatgpt", "ChatGPT")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `Claude exact match returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.anthropic.claude", "Claude")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `Discord exact match returns CAT_SOCIAL`() {
        val app = appInfo("com.discord", "Discord")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    // ─── Online DB Önceliği ───────────────────────────────────────────────────

    @Test
    fun `Online DB overrides exact match`() {
        // Instagram normalde exact match ile social, ama online DB farklı diyorsa kazanır
        every { mockDbService.getCategoryForPackage("com.instagram.android") } returns Category.CAT_OTHER
        val app = appInfo("com.instagram.android", "Instagram")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    @Test
    fun `Online DB result is used when available`() {
        every { mockDbService.getCategoryForPackage("com.unknown.app") } returns Category.CAT_FINANCE
        val app = appInfo("com.unknown.app", "Unknown App")
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(app))
    }

    // ─── Keyword Eşleşmesi ────────────────────────────────────────────────────

    @Test
    fun `App name keyword match - fitness app returns CAT_HEALTH`() {
        val app = appInfo("com.random.dev", "My Fitness Tracker")
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(app))
    }

    @Test
    fun `App name keyword match - bank app returns CAT_FINANCE`() {
        val app = appInfo("tr.random.bankapp", "My Bank App")
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(app))
    }

    @Test
    fun `Package name keyword match - shopping returns CAT_SHOPPING`() {
        val app = appInfo("com.trendyol.android", "Trendyol")
        assertEquals(Category.CAT_SHOPPING, classifier.classifyApp(app))
    }

    @Test
    fun `App name keyword match - game app returns CAT_GAMES`() {
        val app = appInfo("com.random.studio", "Epic Racing Game")
        assertEquals(Category.CAT_GAMES, classifier.classifyApp(app))
    }

    @Test
    fun `Duolingo keyword match returns CAT_EDUCATION`() {
        val app = appInfo("com.duolingo.app", "Duolingo")
        assertEquals(Category.CAT_EDUCATION, classifier.classifyApp(app))
    }

    @Test
    fun `News keyword in app name returns CAT_NEWS`() {
        val app = appInfo("tr.com.myapp", "BBC News")
        assertEquals(Category.CAT_NEWS, classifier.classifyApp(app))
    }

    // ─── Varsayılan ───────────────────────────────────────────────────────────

    @Test
    fun `Unknown app with no match returns CAT_OTHER`() {
        val app = appInfo("com.randomxyz.nothing", "XYZ App 123")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    // ─── Toplu Sınıflandırma ─────────────────────────────────────────────────

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

    // ─── Güven Skoru ─────────────────────────────────────────────────────────

    @Test
    fun `getConfidence exact match returns 95`() {
        val app = appInfo("com.instagram.android", "Instagram")
        assertEquals(95, classifier.getConfidence(app, Category.CAT_SOCIAL))
    }

    @Test
    fun `getConfidence CAT_OTHER returns 30`() {
        val app = appInfo("com.unknown.app", "Unknown")
        assertEquals(30, classifier.getConfidence(app, Category.CAT_OTHER))
    }

    @Test
    fun `getConfidence keyword match returns at least 70`() {
        val app = appInfo("com.fitness.app", "Fitness Tracker")
        val conf = classifier.getConfidence(app, Category.CAT_HEALTH)
        assertTrue("Beklenen >= 70, gerçek: $conf", conf >= 70)
    }

    // ─── Yardımcı ─────────────────────────────────────────────────────────────

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
