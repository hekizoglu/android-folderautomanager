package com.armutlu.apporganizer

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.classify.KeywordDatabase
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

    private lateinit var classifier: AppClassifier

    @Before
    fun setup() {
        classifier = AppClassifier()
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

    // ─── addKeywordToCategory Runtime Ekleme ─────────────────────────────────

    @Test
    fun `addKeywordToCategory adds new keyword and it is used in classification`() {
        // Hiçbir keyword listesinde olmayan benzersiz bir kelime kullan
        val uniqueKeyword = "xyzuniquekw99"
        KeywordDatabase.addKeywordToCategory(Category.CAT_EDUCATION, uniqueKeyword)
        val after = appInfo("com.test.xyzuniquekw99", uniqueKeyword)
        assertEquals(Category.CAT_EDUCATION, classifier.classifyApp(after))
    }

    @Test
    fun `addKeywordToCategory does not add duplicate keyword`() {
        val countBefore = KeywordDatabase.getTotalKeywords()
        KeywordDatabase.addKeywordToCategory(Category.CAT_SOCIAL, "facebook") // zaten var
        val countAfter = KeywordDatabase.getTotalKeywords()
        assertEquals(countBefore, countAfter)
    }

    @Test
    fun `addKeywordToCategory to unknown category increases total keyword count`() {
        val countBefore = KeywordDatabase.getTotalKeywords()
        KeywordDatabase.addKeywordToCategory("cat_zzzcustom_isolated", "zzzisolatedkeyword12345")
        val countAfter = KeywordDatabase.getTotalKeywords()
        assertTrue("Yeni keyword eklenmeli", countAfter > countBefore)
    }

    // ─── Büyük/Küçük Harf Edge Case ──────────────────────────────────────────

    @Test
    fun `ChatGPT uppercase app name still returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.random.ai", "CHATGPT")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `chatgpt lowercase app name returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.random.ai", "chatgpt")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `ChatGPT mixed case app name returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.random.ai", "ChatGPT")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `Telegram all uppercase app name returns CAT_SOCIAL`() {
        val app = appInfo("com.random.messenger", "TELEGRAM")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    // ─── Boş / Null-benzeri Input Güvenliği ──────────────────────────────────

    @Test
    fun `empty app name with unknown package returns CAT_OTHER`() {
        val app = appInfo("com.unknown.pkg", "")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    @Test
    fun `empty package name with unknown app name returns CAT_OTHER`() {
        val app = appInfo("", "ZzqXvbwrst999")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    @Test
    fun `both empty package and app name returns CAT_OTHER`() {
        val app = appInfo("", "")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    @Test
    fun `whitespace-only app name returns CAT_OTHER`() {
        val app = appInfo("com.unknown.pkg", "   ")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    // ─── exactMatchMap Önceliği (keyword override edilemez) ──────────────────

    @Test
    fun `exactMatchMap takes priority over keyword classification`() {
        // org.telegram.messenger → exactMatchMap'de CAT_SOCIAL
        // "messenger" keyword'ü CAT_SOCIAL'da da var, ama exactMatch daha önce çalışır
        val app = appInfo("org.telegram.messenger", "Telegram")
        // Online DB null döndürüyor (setup'ta ayarlı), exactMatch kazanmalı
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `exactMatchMap assigns CAT_PRODUCTIVITY to ChatGPT package despite no keyword match needed`() {
        // com.openai.chatgpt exactMatchMap'de CAT_PRODUCTIVITY
        // Keyword tablosunda da "chatgpt" var → hangi yoldan gelirse gelsin sonuç aynı
        val app = appInfo("com.openai.chatgpt", "SomeApp") // app name farklı ama paket exact match
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `exactMatchMap entry with misleading app name wins over keyword`() {
        // com.discord paketi exactMatchMap'de CAT_SOCIAL
        // App adını "Game Arena" gibi oyun ismine çekersek — exactMatch yine kazanmalı
        val app = appInfo("com.discord", "Game Arena Legends")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    // ─── getConfidence ek senaryolar ─────────────────────────────────────────

    @Test
    fun `getConfidence for unknown category returns 50`() {
        val app = appInfo("com.random.app", "Random App")
        val conf = classifier.getConfidence(app, Category.CAT_FINANCE)
        assertEquals(50, conf)
    }

    @Test
    fun `getConfidence package keyword match returns at least 70`() {
        // "trendyol" paket adında geçiyor → hasPackageKeywordMatch true
        val app = appInfo("com.trendyol.android", "Trendyol")
        val conf = classifier.getConfidence(app, Category.CAT_SHOPPING)
        assertTrue("Beklenen >= 70, gerçek: $conf", conf >= 70)
    }

    // ─── Yardımcı ─────────────────────────────────────────────────────────────

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
