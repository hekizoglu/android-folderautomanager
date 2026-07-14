package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * CategorySuggestionEngine birim testleri (P0.7).
 * Amac: sadece gercek sinyal varken oneri uretilsin - sahte/rastgele oneri yok.
 */
class CategorySuggestionEngineTest {

    private fun app(
        packageName: String,
        appName: String = "Example",
        categoryId: String = Category.CAT_OTHER,
    ): AppInfo = AppInfo(
        packageName = packageName,
        appName = appName,
        categoryId = categoryId,
    )

    @Test
    fun `keyword match on app name returns KEYWORD suggestion`() {
        val target = app(packageName = "com.unknownvendor.xyz", appName = "My Trading Wallet")
        val allApps = listOf(target)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        assertEquals(Category.CAT_FINANCE, suggestion?.categoryId)
        assertEquals(CategorySuggestionEngine.SignalType.KEYWORD, suggestion?.signal)
    }

    @Test
    fun `keyword match on package name returns KEYWORD suggestion`() {
        val target = app(packageName = "com.unknownvendor.shopping.deals", appName = "Deals")
        val allApps = listOf(target)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        assertEquals(Category.CAT_SHOPPING, suggestion?.categoryId)
        assertEquals(CategorySuggestionEngine.SignalType.KEYWORD, suggestion?.signal)
    }

    @Test
    fun `known vendor prefix with existing classified sibling returns VENDOR suggestion`() {
        // com.huawei.* AppClassifier'daki bilinen uretici onegi - VENDOR sinyali icin
        // tek eslesme yeterlidir (bilinen uretici oldugu icin genis eslesme guvenilirdir).
        val target = app(packageName = "com.huawei.zylo", appName = "Zylo")
        val sibling = app(
            packageName = "com.huawei.blorp",
            appName = "Blorp",
            categoryId = Category.CAT_UTILITIES,
        )
        val allApps = listOf(target, sibling)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        assertEquals(Category.CAT_UTILITIES, suggestion?.categoryId)
        assertEquals(CategorySuggestionEngine.SignalType.VENDOR, suggestion?.signal)
    }

    @Test
    fun `similar package prefix with two classified siblings returns SIMILAR_PACKAGE suggestion`() {
        // Ayni ilk 2 segment onekli (com.riverstudio.*) 2 farkli uygulama zaten CAT_ART'a
        // atanmis - hicbiri "vendor" olarak degil, genel "benzer paket" sinyali ile.
        val target = app(packageName = "com.riverstudio.zylo", appName = "Zylo")
        val sib1 = app(packageName = "com.riverstudio.blorp", appName = "Blorp", categoryId = Category.CAT_ART)
        val sib2 = app(packageName = "com.riverstudio.quxo", appName = "Quxo", categoryId = Category.CAT_ART)
        val allApps = listOf(target, sib1, sib2)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        assertEquals(Category.CAT_ART, suggestion?.categoryId)
        assertEquals(CategorySuggestionEngine.SignalType.SIMILAR_PACKAGE, suggestion?.signal)
    }

    @Test
    fun `generic android prefix is filtered out of similar package signal`() {
        val target = app(packageName = "com.android.zylo", appName = "Zylo")
        val sib1 = app(packageName = "com.android.blorp", appName = "Blorp", categoryId = Category.CAT_PRODUCTIVITY)
        val sib2 = app(packageName = "com.android.quxo", appName = "Quxo", categoryId = Category.CAT_PRODUCTIVITY)
        val allApps = listOf(target, sib1, sib2)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        // com.android jenerik onek listesinde oldugundan SIMILAR_PACKAGE sinyali
        // reddedilmeli; bilinen uretici listesinde de olmadigindan VENDOR da tetiklenmez;
        // keyword de eslesmiyor -> null.
        assertNull(suggestion)
    }

    @Test
    fun `single sibling with unknown vendor prefix is not enough for SIMILAR_PACKAGE signal`() {
        val target = app(packageName = "com.riverstudio.zylo", appName = "Zylo")
        val onlySibling = app(packageName = "com.riverstudio.blorp", appName = "Blorp", categoryId = Category.CAT_ART)
        val allApps = listOf(target, onlySibling)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        // com.riverstudio bilinen uretici listesinde degil (VENDOR tetiklenmez) ve
        // SIMILAR_PACKAGE en az 2 kardes uygulama ister - tek eslesme yetersiz -> null.
        assertNull(suggestion)
    }

    @Test
    fun `no signal at all returns null`() {
        val target = app(packageName = "com.zzzzunique1234.qqqqapp", appName = "Qxlmzt")
        val allApps = listOf(target)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        assertNull(suggestion)
    }

    @Test
    fun `siblings still in OTHER or UNCATEGORIZED do not count as vendor signal`() {
        val target = app(packageName = "com.huawei.zylo", appName = "Zylo")
        val sibling = app(
            packageName = "com.huawei.blorp",
            appName = "Blorp",
            categoryId = Category.CAT_OTHER,
        )
        val allApps = listOf(target, sibling)

        val suggestion = CategorySuggestionEngine.suggestFor(target, allApps)

        assertNull(suggestion)
    }
}
