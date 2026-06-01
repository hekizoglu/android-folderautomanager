package com.armutlu.apporganizer.launcher

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.buildAllApps
import com.armutlu.apporganizer.presentation.ui.launcher.buildFolders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * buildFolders ve buildAllApps fonksiyonları için saf birim testleri.
 * Android bağımlılığı yok — local JVM üzerinde çalışır.
 */
class LauncherViewModelLogicTest {

    // ── buildFolders ──────────────────────────────────────────────────────────

    @Test
    fun `buildFolders_bos_liste_donduruyor_bos_girdi_icin`() {
        assertTrue(buildFolders(emptyList()).isEmpty())
    }

    @Test
    fun `buildFolders_uygulamalari_kategoriye_gore_gruplar`() {
        val apps = listOf(
            app("com.instagram.android", "Instagram", "social"),
            app("com.game1", "Game One", "games")
        )
        val folders = buildFolders(apps)
        val socialFolder = folders.first { it.category.categoryId == "social" }
        assertEquals(1, socialFolder.apps.size)
        assertEquals("Instagram", socialFolder.apps[0].appName)
    }

    @Test
    fun `buildFolders_bos_kategorileri_dislar`() {
        val apps = listOf(app("com.instagram", "Instagram", "social"))
        val folders = buildFolders(apps)
        assertFalse("Oyun uygulaması yokken games klasörü olmamalı",
            folders.any { it.category.categoryId == "games" })
    }

    @Test
    fun `buildFolders_uncategorized_klasor_olusturmaz`() {
        val apps = listOf(
            app("com.x", "App X", Category.CAT_UNCATEGORIZED)
        )
        val folders = buildFolders(apps)
        assertFalse(folders.any { it.category.categoryId == Category.CAT_UNCATEGORIZED })
        assertTrue("Kategorisiz uygulama klasör oluşturmamalı", folders.isEmpty())
    }

    @Test
    fun `buildFolders_klasor_icindeki_uygulamalar_ada_gore_sirali`() {
        val apps = listOf(
            app("pkg.z", "Zebra App", "games"),
            app("pkg.a", "Alpha App", "games"),
            app("pkg.m", "Middle App", "games")
        )
        val folder = buildFolders(apps).first { it.category.categoryId == "games" }
        assertEquals(listOf("Alpha App", "Middle App", "Zebra App"),
            folder.apps.map { it.appName })
    }

    @Test
    fun `buildFolders_klasorler_displayOrder_a_gore_sirali`() {
        // social displayOrder=1, games displayOrder=3
        val apps = listOf(
            app("p1", "App1", "games"),
            app("p2", "App2", "social")
        )
        val folders = buildFolders(apps)
        val socialIdx = folders.indexOfFirst { it.category.categoryId == "social" }
        val gamesIdx  = folders.indexOfFirst { it.category.categoryId == "games" }
        assertTrue("Social (order=1) games'ten (order=3) önce gelmeli", socialIdx < gamesIdx)
    }

    @Test
    fun `buildFolders_birden_fazla_uygulama_ayni_kategoride`() {
        val apps = listOf(
            app("a", "App A", "social"),
            app("b", "App B", "social"),
            app("c", "App C", "social")
        )
        val folder = buildFolders(apps).first { it.category.categoryId == "social" }
        assertEquals(3, folder.apps.size)
    }

    @Test
    fun `buildFolders_tum_kategoriler_dolu_olabilir`() {
        val allCats = Category.getDefaultCategories()
            .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        val apps = allCats.mapIndexed { i, cat ->
            app("pkg.$i", "App $i", cat.categoryId)
        }
        val folders = buildFolders(apps)
        assertEquals(allCats.size, folders.size)
    }

    @Test
    fun `buildFolders_kategori_nesnesi_dogru_ataniyor`() {
        val apps = listOf(app("com.x", "SomeApp", "social"))
        val folder = buildFolders(apps).first()
        assertEquals("social", folder.category.categoryId)
        assertEquals("Sosyal Medya", folder.category.categoryName)
        assertEquals("👥", folder.category.iconEmoji)
    }

    // ── buildAllApps ──────────────────────────────────────────────────────────

    @Test
    fun `buildAllApps_bos_liste_donduruyor_bos_girdi_icin`() {
        assertTrue(buildAllApps(emptyList()).isEmpty())
    }

    @Test
    fun `buildAllApps_ada_gore_siralar`() {
        val apps = listOf(
            app("c", "Charlie", "social"),
            app("a", "Alice", "social"),
            app("b", "Bob", "social")
        )
        val sorted = buildAllApps(apps)
        assertEquals(listOf("Alice", "Bob", "Charlie"), sorted.map { it.appName })
    }

    @Test
    fun `buildAllApps_tum_kategorileri_dahil_eder`() {
        val apps = listOf(
            app("a", "App A", "social"),
            app("b", "App B", "games"),
            app("c", "App C", Category.CAT_UNCATEGORIZED)
        )
        assertEquals(3, buildAllApps(apps).size)
    }

    @Test
    fun `buildAllApps_buyuk_harf_kucuk_harf_siralamayi_etkilemez`() {
        val apps = listOf(
            app("z", "zoom", "social"),
            app("a", "Apple", "social")
        )
        // Kotlin sortedBy kullanır — unicode-aware, büyük/küçük harf bağımsız
        val sorted = buildAllApps(apps)
        // "Apple" < "zoom" alfabetik sırayla
        assertEquals("Apple", sorted[0].appName)
        assertEquals("zoom", sorted[1].appName)
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    private fun app(pkg: String, name: String, categoryId: String) = AppInfo(
        packageName = pkg,
        appName     = name,
        categoryId  = categoryId
    )
}
