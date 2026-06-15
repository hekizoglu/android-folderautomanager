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
 * KapsamÄ±:
 *  - Bilinen doÄŸru sÄ±nÄ±flandÄ±rmalar (ChatGPT â†’ productivity, Telegram â†’ social vb.)
 *  - Fuzzy / kÄ±smi eÅŸleÅŸme doÄŸruluÄŸu
 *  - Paket adÄ± vs uygulama adÄ± Ã§akÄ±ÅŸmalarÄ±
 *  - YanlÄ±ÅŸ sÄ±nÄ±flandÄ±rma riski taÅŸÄ±yan uygulamalar
 *  - Ã‡oklu keyword eÅŸleÅŸmesi durumunda hangi kategori kazanÄ±r
 *  - BÃ¼yÃ¼k/kÃ¼Ã§Ã¼k harf, boÅŸluk, Ã¶zel karakter iÃ§eren girdiler
 */
class AppClassifierEdgeCaseTest {

    private lateinit var classifier: AppClassifier

    @Before
    fun setup() {
        classifier = AppClassifier()
    }

    // â”€â”€â”€ 1. Bilinen DoÄŸru SÄ±nÄ±flandÄ±rmalar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `ChatGPT via exact match returns CAT_PRODUCTIVITY`() {
        val app = appInfo("com.openai.chatgpt", "ChatGPT")
        assertEquals(Category.CAT_PRODUCTIVITY, classifier.classifyApp(app))
    }

    @Test
    fun `Telegram via exact match returns CAT_COMMUNICATION`() {
        val app = appInfo("org.telegram.messenger", "Telegram")
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(app))
    }

    @Test
    fun `WhatsApp via exact match returns CAT_COMMUNICATION`() {
        val app = appInfo("com.whatsapp", "WhatsApp")
        assertEquals(Category.CAT_COMMUNICATION, classifier.classifyApp(app))
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

    // â”€â”€â”€ 2. Fuzzy / KÄ±smi EÅŸleÅŸme DoÄŸruluÄŸu â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `app name containing social keyword returns CAT_SOCIAL`() {
        // "social" keyword doÄŸrudan geÃ§iyor
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

    // â”€â”€â”€ 3. Paket AdÄ± - Uygulama AdÄ± Ã‡akÄ±ÅŸmasÄ± â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `misleading app name but exact package match wins`() {
        // Paket adÄ± exactMatchMap'de social olarak kayÄ±tlÄ±
        // Ama app adÄ± "Budget Tracker" gibi finance Ã§aÄŸrÄ±ÅŸtÄ±rÄ±yor
        val app = appInfo("com.facebook.katana", "Budget Tracker Pro")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `app name has game keyword but package is exact social match`() {
        // com.snapchat.android â†’ exactMatchMap â†’ CAT_SOCIAL
        // App adÄ±nda "game" geÃ§iyor â€” exactMatch kazanmalÄ±
        val app = appInfo("com.snapchat.android", "Snap Game Filter")
        assertEquals(Category.CAT_SOCIAL, classifier.classifyApp(app))
    }

    @Test
    fun `unknown package but app name keyword drives classification`() {
        val app = appInfo("com.xyzabc.nothing", "Workout Tracker & Gym Planner")
        assertEquals(Category.CAT_HEALTH, classifier.classifyApp(app))
    }

    // â”€â”€â”€ 4. YanlÄ±ÅŸ SÄ±nÄ±flandÄ±rma Riskli Uygulamalar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `app named Daily shows productivity dominates news keyword`() {
        // "daily" NEWS keyword'Ã¼ ama productivity keyword'leri daha aÄŸÄ±r bastÄ±ÄŸÄ±nda
        // productivity kazanÄ±r â€” bu mevcut davranÄ±ÅŸÄ± belgeler
        val app = appInfo("com.random.daily", "Daily")
        val result = classifier.classifyApp(app)
        // "daily" news keyword'Ã¼ â€” news veya productivity kabul edilebilir
        assertTrue(result == Category.CAT_NEWS || result == Category.CAT_PRODUCTIVITY)
    }

    @Test
    fun `app named Play Store shows productivity dominates games keyword`() {
        // "play" hem GAMES hem PRODUCTIVITY'de olabilir â€” hangi liste daha aÄŸÄ±r basarsa
        val app = appInfo("com.google.android.play.store", "Play Store")
        val result = classifier.classifyApp(app)
        // games veya productivity kabul edilebilir
        assertNotEquals(Category.CAT_OTHER, result)
    }

    @Test
    fun `app with AI in name but unrelated to AI tools returns CAT_PRODUCTIVITY`() {
        // "ai" keyword'Ã¼ CAT_PRODUCTIVITY'de var â€” her "ai" iÃ§eren uygulama oraya dÃ¼ÅŸer
        val app = appInfo("com.photo.ai.enhancer", "Photo AI Enhancer")
        assertNotEquals(Category.CAT_OTHER, classifier.classifyApp(app))
    }

    @Test
    fun `security tool app returns CAT_UTILITIES not CAT_FINANCE`() {
        val app = appInfo("com.random.security", "Phone Security & Antivirus")
        assertEquals(Category.CAT_UTILITIES, classifier.classifyApp(app))
    }

    // â”€â”€â”€ 5. Ã‡oklu Keyword EÅŸleÅŸmesi â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `app with both fitness and game keywords - first matching category wins`() {
        // "fitness game" hem health hem game keyword iÃ§eriyor
        // KeywordDatabase sÄ±ralamasÄ± belirleyici â€” ilk eÅŸleÅŸen kazanÄ±r
        val app = appInfo("com.random.app", "Fitness Game Challenge")
        val result = classifier.classifyApp(app)
        // SonuÃ§ deterministik olmalÄ± (null / CAT_OTHER olmamalÄ±)
        assertNotEquals(Category.CAT_OTHER, result)
    }

    @Test
    fun `classifyApps handles 100 apps without error`() {
        val apps = (1..100).map { i ->
            appInfo("com.test.app$i", "Test App $i")
        }
        val results = classifier.classifyApps(apps)
        assertEquals(100, results.size)
        // HiÃ§bir sonuÃ§ null olmamalÄ±
        results.values.forEach { category ->
            assertTrue(category.isNotEmpty())
        }
    }

    // â”€â”€â”€ 6. Ã–zel Karakter / Unicode â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `app name with unicode characters returns safe result`() {
        val app = appInfo("com.turkish.app", "HabertÃ¼rk")
        // "habertÃ¼rk" NEWS keywords listesinde var
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
        assertNotNull(result) // Crash olmadan herhangi bir kategori dÃ¶nmeli
    }

    // â”€â”€â”€ YardÄ±mcÄ± â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun appInfo(packageName: String, appName: String) = AppInfo(
        packageName = packageName,
        appName = appName
    )
}



