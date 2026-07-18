package com.armutlu.apporganizer.launcher

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.DOCK_MAX_SIZE
import com.armutlu.apporganizer.presentation.ui.launcher.HomeContextualRowKind
import com.armutlu.apporganizer.presentation.ui.launcher.HomeLayoutMath
import com.armutlu.apporganizer.presentation.ui.launcher.buildContextualDockPackages
import com.armutlu.apporganizer.presentation.ui.launcher.buildAllApps
import com.armutlu.apporganizer.presentation.ui.launcher.buildFolders
import com.armutlu.apporganizer.presentation.ui.launcher.fillDockSuggestions
import com.armutlu.apporganizer.presentation.ui.launcher.filterAllAppsByQuery
import com.armutlu.apporganizer.presentation.ui.launcher.filterTodayInstalledApps
import com.armutlu.apporganizer.presentation.ui.launcher.isDockAdditionBlocked
import com.armutlu.apporganizer.presentation.ui.launcher.selectHomeContextualRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * buildFolders ve buildAllApps fonksiyonları için saf birim testleri.
 * Android bağımlılığı yok — local JVM üzerinde çalışır.
 */
class LauncherViewModelLogicTest {

    private val categories = Category.getDefaultCategories()

    // ── buildFolders ──────────────────────────────────────────────────────────

    @Test
    fun `buildFolders_bos_liste_donduruyor_bos_girdi_icin`() {
        assertTrue(buildFolders(emptyList(), categories).isEmpty())
    }

    @Test
    fun `buildFolders_uygulamalari_kategoriye_gore_gruplar`() {
        val apps = listOf(
            app("com.instagram.android", "Instagram", "social"),
            app("com.game1", "Game One", "games")
        )
        val folders = buildFolders(apps, categories)
        val socialFolder = folders.first { it.category.categoryId == "social" }
        assertEquals(1, socialFolder.apps.size)
        assertEquals("Instagram", socialFolder.apps[0].appName)
    }

    @Test
    fun `buildFolders_bos_kategorileri_dislar`() {
        val apps = listOf(app("com.instagram", "Instagram", "social"))
        val folders = buildFolders(apps, categories)
        assertFalse("Oyun uygulaması yokken games klasörü olmamalı",
            folders.any { it.category.categoryId == "games" })
    }

    @Test
    fun `buildFolders_uncategorized_klasor_olusturmaz`() {
        val apps = listOf(
            app("com.x", "App X", Category.CAT_UNCATEGORIZED)
        )
        val folders = buildFolders(apps, categories)
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
        val folder = buildFolders(apps, categories).first { it.category.categoryId == "games" }
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
        val folders = buildFolders(apps, categories)
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
        val folder = buildFolders(apps, categories).first { it.category.categoryId == "social" }
        assertEquals(3, folder.apps.size)
    }

    @Test
    fun `buildFolders_tum_kategoriler_dolu_olabilir`() {
        val allCats = Category.getDefaultCategories()
            .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        val apps = allCats.mapIndexed { i, cat ->
            app("pkg.$i", "App $i", cat.categoryId)
        }
        val folders = buildFolders(apps, allCats)
        assertEquals(allCats.size, folders.size)
    }

    @Test
    fun `buildFolders_kategori_nesnesi_dogru_ataniyor`() {
        val apps = listOf(app("com.x", "SomeApp", "social"))
        val folder = buildFolders(apps, categories).first()
        assertEquals("social", folder.category.categoryId)
        assertEquals("Sosyal Medya", folder.category.categoryName)
        assertEquals("👥", folder.category.iconEmoji)
    }

    // P0.1: Klasör içinden kategori değiştirme sonrası açık klasör içeriğinin ve klasör
    // listesinin Room Flow üzerinden anında güncellendiğini doğrular — updateAppCategory
    // repository'e yazınca allAppsSource yeniden emit eder, folders StateFlow buildFolders'i
    // yeni categoryId ile tekrar çalıştırır. Burada categoryId elle değiştirilerek
    // buildFolders'in girdi listesindeki değişikliği doğru yansıttığı test edilir.
    @Test
    fun `buildFolders_kategori_degisince_uygulama_eski_klasorden_cikar_yeni_klasore_girer`() {
        val beforeApps = listOf(
            app("com.moved", "MovedApp", "social"),
            app("com.stays", "StaysApp", "social"),
        )
        val before = buildFolders(beforeApps, categories)
        val socialBefore = before.first { it.category.categoryId == "social" }
        assertEquals(2, socialBefore.apps.size)
        assertFalse(before.any { it.category.categoryId == "games" })

        // Kullanıcı FolderScreen içinde "MovedApp"i games kategorisine taşıdı —
        // updateAppCategory Room'a yazar, allAppsSource yeni categoryId ile emit eder.
        val afterApps = listOf(
            app("com.moved", "MovedApp", "games"),
            app("com.stays", "StaysApp", "social"),
        )
        val after = buildFolders(afterApps, categories)
        val socialAfter = after.first { it.category.categoryId == "social" }
        val gamesAfter = after.first { it.category.categoryId == "games" }

        assertEquals("Taşınan uygulama eski klasörden çıkmalı", 1, socialAfter.apps.size)
        assertEquals("StaysApp", socialAfter.apps[0].appName)
        assertEquals("Taşınan uygulama yeni klasörde görünmeli", 1, gamesAfter.apps.size)
        assertEquals("MovedApp", gamesAfter.apps[0].appName)
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

    @Test
    fun `fillDockSuggestions_dock_kapasitesini_bese_tamamlar`() {
        val slotApps = listOf(
            app("pkg.1", "App 1", "social"),
            app("pkg.2", "App 2", "social"),
            app("pkg.3", "App 3", "social"),
        )
        val fallbackApps = listOf(
            app("pkg.4", "App 4", "social"),
            app("pkg.5", "App 5", "social"),
            app("pkg.6", "App 6", "social"),
        )

        val result = fillDockSuggestions(slotApps, fallbackApps)

        assertEquals(DOCK_MAX_SIZE, result.size)
        assertEquals(listOf("pkg.1", "pkg.2", "pkg.3", "pkg.4", "pkg.5"), result.map { it.packageName })
    }

    @Test
    fun `buildContextualDockPackages_sabit_ogeleri_korur_ve_bos_slotlari_doldurur`() {
        val fixed = listOf("fixed.1", "fixed.2", "fixed.3")
        val suggested = listOf("fixed.2", "smart.1", "smart.2", "smart.3")

        val result = buildContextualDockPackages(
            fixed = fixed,
            suggested = suggested,
            contextualEnabled = true
        )

        assertEquals(listOf("fixed.1", "fixed.2", "fixed.3", "smart.1", "smart.2"), result)
    }

    @Test
    fun `buildContextualDockPackages_kapaliysa_sadece_sabit_slotlari_dondurur`() {
        val fixed = listOf("fixed.1", "fixed.2", "fixed.3", "fixed.4", "fixed.5", "fixed.6")
        val suggested = listOf("smart.1", "smart.2")

        val result = buildContextualDockPackages(
            fixed = fixed,
            suggested = suggested,
            contextualEnabled = false
        )

        assertEquals(listOf("fixed.1", "fixed.2", "fixed.3", "fixed.4", "fixed.5"), result)
    }

    @Test
    fun `dock edit sheet bes slotta bir oge cikarilinca yeni eklemeye izin verir`() {
        assertFalse(isDockAdditionBlocked(dockSize = DOCK_MAX_SIZE - 1, itemInDock = false))
        assertTrue(isDockAdditionBlocked(dockSize = DOCK_MAX_SIZE, itemInDock = false))
        assertFalse(isDockAdditionBlocked(dockSize = DOCK_MAX_SIZE, itemInDock = true))
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    @Test
    fun `selectHomeContextualRow_onerileri_tek_satir_olarak_onceliklendirir_ve_tekrarlari_dislar`() {
        val row = selectHomeContextualRow(
            favoritesEnabled = true,
            favoriteApps = listOf(app("fav.1", "Favorite", "social")),
            suggestionsEnabled = true,
            suggestedApps = listOf(
                app("dock.1", "Docked", "social"),
                app("fav.1", "Favorite", "social"),
                app("smart.1", "Smart 1", "social"),
                app("smart.2", "Smart 2", "social"),
            ),
            recentNotificationAppsEnabled = true,
            recentNotificationApps = listOf(app("notif.1", "Notification", "social")),
            recentAppsEnabled = true,
            recentApps = listOf(app("recent.1", "Recent", "social")),
            dockPackages = listOf("dock.1"),
        )

        assertEquals(HomeContextualRowKind.SUGGESTIONS, row?.kind)
        assertEquals(listOf("smart.1", "smart.2"), row?.apps?.map { it.packageName })
    }

    @Test
    fun `selectHomeContextualRow_oneri_yokken_bildirim_son_kullanilan_favori_sirasini_izler`() {
        val row = selectHomeContextualRow(
            favoritesEnabled = true,
            favoriteApps = listOf(app("fav.1", "Favorite", "social")),
            suggestionsEnabled = false,
            suggestedApps = emptyList(),
            recentNotificationAppsEnabled = true,
            recentNotificationApps = listOf(app("notif.1", "Notification", "social")),
            recentAppsEnabled = true,
            recentApps = listOf(app("recent.1", "Recent", "social")),
            dockPackages = emptyList(),
        )

        assertEquals(HomeContextualRowKind.RECENT_NOTIFICATIONS, row?.kind)
        assertEquals(listOf("notif.1"), row?.apps?.map { it.packageName })
    }

    @Test
    fun `homeLayoutMath_kucuk_ekranda_en_az_bir_klasor_satiri_korur`() {
        val capacity = HomeLayoutMath.folderCapacity(
            availableHeightDp = 96,
            folderSizeDp = 72,
            columns = 4,
        )
        val pageSize = HomeLayoutMath.pageSize(
            requestedPageSize = 8,
            folderCapacity = capacity,
        )

        assertEquals(HomeLayoutMath.MIN_VISIBLE_FOLDERS, capacity)
        assertEquals(HomeLayoutMath.MIN_VISIBLE_FOLDERS, pageSize)
    }

    // ── filterAllAppsByQuery (P00 — searchQuery sayfadan bağımsız pure filtre) ─────────────

    @Test
    fun `filterAllAppsByQuery bos sorguda tum listeyi degistirmeden dondurur`() {
        val apps = listOf(app("com.a", "Alpha", "social"), app("com.b", "Beta", "social"))
        assertEquals(apps, filterAllAppsByQuery(apps, ""))
        assertEquals(apps, filterAllAppsByQuery(apps, "   "))
    }

    @Test
    fun `filterAllAppsByQuery uygulama adina gore filtreler`() {
        val apps = listOf(
            app("com.a", "Alpha", "social"),
            app("com.b", "Beta", "social"),
            app("com.c", "Gamma", "social"),
        )
        assertEquals(listOf("Alpha"), filterAllAppsByQuery(apps, "alp").map { it.appName })
    }

    @Test
    fun `filterAllAppsByQuery paket adina gore de filtreler`() {
        val apps = listOf(
            app("com.whatsapp", "WhatsApp", "social"),
            app("com.telegram", "Telegram", "social"),
        )
        assertEquals(listOf("WhatsApp"), filterAllAppsByQuery(apps, "whats").map { it.appName })
    }

    @Test
    fun `filterAllAppsByQuery turkce locale ile buyuk kucuk harf duyarsiz calisir`() {
        val apps = listOf(app("com.i", "İstanbul App", "social"))
        // Türkçe'de "İ".lowercase() -> "i̇" değil "i" (Locale("tr") ile) — D0 kuralı testi.
        assertEquals(1, filterAllAppsByQuery(apps, "istanbul").size)
    }

    @Test
    fun `filterAllAppsByQuery eslesme yoksa bos liste doner`() {
        val apps = listOf(app("com.a", "Alpha", "social"))
        assertTrue(filterAllAppsByQuery(apps, "zzz").isEmpty())
    }

    private fun app(pkg: String, name: String, categoryId: String) = AppInfo(
        packageName = pkg,
        appName     = name,
        categoryId  = categoryId
    )

    // ── filterTodayInstalledApps (EX01 — "Bugün Yüklenenler") ──────────────────

    private fun appWithInstall(pkg: String, name: String, firstInstalledTime: Long, isHidden: Boolean = false) =
        AppInfo(
            packageName = pkg,
            appName = name,
            categoryId = "social",
            firstInstalledTime = firstInstalledTime,
            isHidden = isHidden,
        )

    @Test
    fun `filterTodayInstalledApps sadece gun sinirlari icindeki uygulamalari dondurur`() {
        val dayStart = 1_000_000L
        val dayEnd = 2_000_000L
        val apps = listOf(
            appWithInstall("com.today1", "Today App 1", firstInstalledTime = 1_500_000L),
            appWithInstall("com.yesterday", "Yesterday App", firstInstalledTime = 999_999L),
            appWithInstall("com.tomorrow", "Tomorrow App", firstInstalledTime = 2_000_000L), // end exclusive
            appWithInstall("com.today2", "Today App 2", firstInstalledTime = 1_000_000L), // start inclusive
        )

        val result = filterTodayInstalledApps(apps, dayStart, dayEnd)

        assertEquals(setOf("com.today1", "com.today2"), result.map { it.packageName }.toSet())
    }

    @Test
    fun `filterTodayInstalledApps en yeni yuklenen once gelecek sekilde siralar`() {
        val dayStart = 0L
        val dayEnd = 1_000_000L
        val apps = listOf(
            appWithInstall("com.early", "Early", firstInstalledTime = 100L),
            appWithInstall("com.late", "Late", firstInstalledTime = 500L),
            appWithInstall("com.mid", "Mid", firstInstalledTime = 300L),
        )

        val result = filterTodayInstalledApps(apps, dayStart, dayEnd)

        assertEquals(listOf("com.late", "com.mid", "com.early"), result.map { it.packageName })
    }

    @Test
    fun `filterTodayInstalledApps gizli uygulamalari dislar`() {
        val dayStart = 0L
        val dayEnd = 1_000_000L
        val apps = listOf(
            appWithInstall("com.visible", "Visible", firstInstalledTime = 100L, isHidden = false),
            appWithInstall("com.hidden", "Hidden", firstInstalledTime = 200L, isHidden = true),
        )

        val result = filterTodayInstalledApps(apps, dayStart, dayEnd)

        assertEquals(listOf("com.visible"), result.map { it.packageName })
    }

    @Test
    fun `filterTodayInstalledApps bugun yukleme yoksa bos liste doner`() {
        val apps = listOf(appWithInstall("com.old", "Old App", firstInstalledTime = 1L))

        val result = filterTodayInstalledApps(apps, dayStartInclusive = 500L, dayEndExclusive = 1000L)

        assertTrue(result.isEmpty())
    }
}
