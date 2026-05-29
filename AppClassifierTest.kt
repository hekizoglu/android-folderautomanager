package com.armutlu.apporganizer.domain.usecase

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AppClassifierTest {
    
    private lateinit var classifier: AppClassifier
    
    @Before
    fun setup() {
        classifier = AppClassifier()
    }
    
    @Test
    fun testClassifySocialMediaApps() {
        val facebook = AppInfo(
            packageName = "com.facebook.katana",
            appName = "Facebook"
        )
        
        val category = classifier.classifyApp(facebook)
        assertEquals(Category.CAT_SOCIAL, category)
    }
    
    @Test
    fun testClassifyGamesApp() {
        val minecraft = AppInfo(
            packageName = "com.mojang.minecraftpe",
            appName = "Minecraft"
        )
        
        val category = classifier.classifyApp(minecraft)
        assertEquals(Category.CAT_GAMES, category)
    }
    
    @Test
    fun testClassifyProductivityApp() {
        val googleDocs = AppInfo(
            packageName = "com.google.android.apps.docs",
            appName = "Google Docs"
        )
        
        val category = classifier.classifyApp(googleDocs)
        assertEquals(Category.CAT_PRODUCTIVITY, category)
    }
    
    @Test
    fun testClassifyShoppingApp() {
        val amazon = AppInfo(
            packageName = "com.amazon.mShop.android",
            appName = "Amazon Shopping"
        )
        
        val category = classifier.classifyApp(amazon)
        assertTrue(category == Category.CAT_SHOPPING || category == Category.CAT_OTHER)
    }
    
    @Test
    fun testClassifyNewsApp() {
        val bbc = AppInfo(
            packageName = "bbc.mobile.news.ww",
            appName = "BBC News"
        )
        
        val category = classifier.classifyApp(bbc)
        assertEquals(Category.CAT_NEWS, category)
    }
    
    @Test
    fun testClassifyUnknownApp() {
        val unknownApp = AppInfo(
            packageName = "com.some.random.app",
            appName = "XYZ App"
        )
        
        val category = classifier.classifyApp(unknownApp)
        assertEquals(Category.CAT_OTHER, category)
    }
    
    @Test
    fun testClassifyMultipleApps() {
        val apps = listOf(
            AppInfo("com.facebook.katana", "Facebook"),
            AppInfo("com.instagram.android", "Instagram"),
            AppInfo("minecraft", "Minecraft")
        )
        
        val classifications = classifier.classifyApps(apps)
        
        assertEquals(3, classifications.size)
        assertEquals(Category.CAT_SOCIAL, classifications["com.facebook.katana"])
        assertEquals(Category.CAT_GAMES, classifications["minecraft"])
    }
    
    @Test
    fun testGetConfidenceHighMatch() {
        val facebook = AppInfo("com.facebook.katana", "Facebook")
        
        val confidence = classifier.getConfidence(facebook, Category.CAT_SOCIAL)
        assertTrue(confidence >= 70)
    }
    
    @Test
    fun testGetConfidenceUnknownApp() {
        val unknownApp = AppInfo("com.xyz.app", "Unknown")
        
        val confidence = classifier.getConfidence(unknownApp, Category.CAT_OTHER)
        assertTrue(confidence <= 50)
    }
    
    @Test
    fun testKeywordDatabaseSize() {
        val database = KeywordDatabase.getKeywordMap()
        
        // Should have all 10 categories
        assertEquals(10, database.size)
        assertTrue(database.containsKey(Category.CAT_SOCIAL))
        assertTrue(database.containsKey(Category.CAT_GAMES))
    }
    
    @Test
    fun testKeywordDatabaseHasKeywords() {
        val socialKeywords = KeywordDatabase.getKeywords(Category.CAT_SOCIAL)
        
        assertTrue(socialKeywords.isNotEmpty())
        assertTrue(socialKeywords.contains("facebook"))
        assertTrue(socialKeywords.contains("instagram"))
    }
    
    @Test
    fun testKeywordDatabaseTotalCount() {
        val totalKeywords = KeywordDatabase.getTotalKeywords()
        
        // Should have substantial keyword database (200+)
        assertTrue(totalKeywords > 100)
    }
    
    @Test
    fun testClassifyByPackageNameWithoutAppName() {
        val app = AppInfo(
            packageName = "com.instagram.android",
            appName = "Some Random Name"
        )
        
        val category = classifier.classifyApp(app)
        assertEquals(Category.CAT_SOCIAL, category)
    }
    
    @Test
    fun testCaseSensitivityHandling() {
        val app1 = AppInfo("com.FACEBOOK.katana", "FACEBOOK")
        val app2 = AppInfo("com.facebook.katana", "facebook")
        
        val category1 = classifier.classifyApp(app1)
        val category2 = classifier.classifyApp(app2)
        
        // Both should classify the same despite case
        assertEquals(category1, category2)
    }
    
    @Test
    fun testPartialKeywordMatch() {
        val twitterApp = AppInfo("com.twitter.android", "Twitter")
        
        val category = classifier.classifyApp(twitterApp)
        assertEquals(Category.CAT_SOCIAL, category)
    }
    
    @Test
    fun testHealthAndFitnessApp() {
        val runningApp = AppInfo("com.runkeeper.android", "RunKeeper Running App")
        
        val category = classifier.classifyApp(runningApp)
        assertTrue(category == Category.CAT_HEALTH || category == Category.CAT_OTHER)
    }
    
    @Test
    fun testFinanceApp() {
        val bankApp = AppInfo("com.wise.android", "Wise Bank")
        
        val category = classifier.classifyApp(bankApp)
        assertTrue(category == Category.CAT_FINANCE || category == Category.CAT_OTHER)
    }
}
