package com.armutlu.apporganizer

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.classify.KeywordDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AppClassifier edge case testleri.
 *
 * Kapsamı:
 *  - Bilinen doğru sınıflandırmalar (ChatGPT → productivity, Telegram → social vb.)
 *  - Fuzzy / kısmi eşleşme doğruluğu
 *  - Paket adı vs uygulama adı çakışmaları
 *  - Yanlış sınıflandırma riski taşıyan uygulamalar
 *  - Çoklu keyword eşleşmesi durumunda hangi kategori kazanır
 *  - Büyük/küçük harf, boşluk, özel karakter içeren girdiler
 */
class AppClassifierEdgeCaseTest {

    private lateinit var classifier: AppClassifier

    @Before
    fun setup() {
        classifier = AppClassifier()
    }

    // ─── 1. Bilinen Doğru Sınıflandırmalar ───────────────────────────────────

    @Test
    fun `ChatGPT via exact match returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.openai.chatgpt", "ChatGPT")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `Telegram via exact match returns CAT_SOCIAL`() {
        val app = appInfo("org.telegram.messenger", "Telegram")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `WhatsApp via exact match returns CAT_SOCIAL`() {
        val app = appInfo("com.whatsapp", "WhatsApp")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `DeepSeek via exact match returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.deepseek.app", "DeepSeek")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `Microsoft Copilot via exact match returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.microsoft.copilot", "Microsoft Copilot")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    // ─── 2. Fuzzy / Kısmi Eşleşme Doğruluğu ─────────────────────────────────

    @Test
    fun `app name containing social keyword returns CAT_SOCIAL`() {
        // "social" keyword doğrudan geçiyor
        val app = appInfo("com.random.app", "Social Connect Hub")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `app name with partial package keyword match - reddit returns CAT_SOCIAL`() {
        val app = appInfo("com.reddit.frontpage", "Reddit")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `app name containing news keyword returns CAT_NEWS`() {
        val app = appInfo("com.random.pkg", "Breaking News Reader")
        assertEquals(Category.CAT_NEWS, classifier.classifyApp(app))
    }

    @Test
    fun `app name containing health keyword yoga returns CAT_HEALTH`() {
        val app = appInfo("com.yoga.app", "Yoga Practice & Meditation")
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(app))
    }

    @Test
    fun `package name containing crypto keyword returns CAT_FINANCE`() {
        val app = appInfo("com.binance.crypto.wallet", "Binance")
        assertEquals(Category.CAT_FINANCE, classifier.classifyApp(app))
    }

    // ─── 3. Paket Adı - Uygulama Adı Çakışması ───────────────────────────────

    @Test
    fun `misleading app name but exact package match wins`() {
        // Paket adı exactMatchMap'de social olarak kayıtlı
        // Ama app adı "Budget Tracker" gibi finance çağrıştırıyor
        val app = appInfo("com.facebook.katana", "Budget Tracker Pro")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `app name has game keyword but package is exact social match`() {
        // com.snapchat.android → exactMatchMap → CAT_SOCIAL
        // App adında "game" geçiyor — exactMatch kazanmalı
        val app = appInfo("com.snapchat.android", "Snap Game Filter")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `unknown package but app name keyword drives classification`() {
        val app = appInfo("com.xyzabc.nothing", "Workout Tracker & Gym Planner")
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(app))
    }

    // ─── 4. Yanlış Sınıflandırma Riskli Uygulamalar ──────────────────────────

    @Test
    fun `app named Daily shows productivity dominates news keyword`() {
        // "daily" NEWS keyword'ü ama productivity keyword'leri daha ağır bastığında
        // productivity kazanır — bu mevcut davranışı belgeler
        val app = appInfo("com.random.daily", "Daily")
        val result = classifier.classifyApp(app)
        // "daily" news keyword'ü — news veya productivity kabul edilebilir
        assertTrue(result == Category.CAT_NEWS || result == Category.CAT_PRODUCTIVITY)
    }

    @Test
    fun `app named Play Store shows productivity dominates games keyword`() {
        // "play" hem GAMES hem PRODUCTIVITY'de olabilir — hangi liste daha ağır basarsa
        val app = appInfo("com.google.android.play.store", "Play Store")
        val result = classifier.classifyApp(app)
        // games veya productivity kabul edilebilir
        assertTrue(result == Category.CAT_GAMES || result == Category.CAT_PRODUCTIVITY)
    }

    @Test
    fun `app with AI in name but unrelated to AI tools returns CAT_PRODUCTIVITY`() {
        // "ai" keyword'ü CAT_PRODUCTIVITY'de var — her "ai" içeren uygulama oraya düşer
        val app = appInfo("com.photo.ai.enhancer", "Photo AI Enhancer")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `security tool app returns CAT_UTILITIES not CAT_FINANCE`() {
        val app = appInfo("com.random.security", "Phone Security & Antivirus")
        assertEquals(Category.CAT_UTILITIES, classifier.classifyApp(app))
    }

    // ─── 5. Çoklu Keyword Eşleşmesi ──────────────────────────────────────────

    @Test
    fun `app with both fitness and game keywords - first matching category wins`() {
        // "fitness game" hem health hem game keyword içeriyor
        // KeywordDatabase sıralaması belirleyici — ilk eşleşen kazanır
        val app = appInfo("com.random.app", "Fitness Game Challenge")
        val result = classifier.classifyApp(app)
        // Sonuç deterministik olmalı (null / CAT_OTHER olmamalı)
        assertNotEquals(Category.CAT_OTHER, result)
    }

    @Test
    fun `classifyApps handles 100 apps without error`() {
        val apps = (1..100).map { i ->
            appInfo("com.test.app$i", "Test App $i")
        }
        val results = classifier.classifyApps(apps)
        assertEquals(100, results.size)
        // Hiçbir sonuç null olmamalı
        results.values.forEach { category ->
            assertTrue(category.isNotEmpty())
        }
    }

    // ─── 6. Özel Karakter / Unicode ──────────────────────────────────────────

    @Test
    fun `app name with unicode characters returns safe result`() {
        val app = appInfo("com.turkish.app", "Habertürk")
        // "habertürk" NEWS keywords listesinde var
        assertEquals(Category.CAT_NEWS, classifier.classifyApp(app))
    }

    @Test
    fun `app name with numbers only returns CAT_OTHER`() {
        val app = appInfo("com.app.123456", "123456")
        assertEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    @Test
    fun `app name with special characters does not crash`() {
        val app = appInfo("com.app.test", "!@#\$%^&*()")
        val result = classifier.classifyApp(app)
        assertNotNull(result) // Crash olmadan herhangi bir kategori dönmeli
    }

    // ─── Yardımcı ─────────────────────────────────────────────────────────────

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}
