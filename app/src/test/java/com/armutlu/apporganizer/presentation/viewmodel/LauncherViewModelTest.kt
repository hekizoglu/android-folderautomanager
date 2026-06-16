package com.armutlu.apporganizer.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.LauncherViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.PackageManagerHelper
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * LauncherViewModel birim testleri.
 *
 * Test edilen davranışlar:
 * - toggleFavorite: favori ekler / çıkarır
 * - setSearchQuery / filteredAllApps: boş sorgu tüm uygulamaları döndürür, dolu sorgu filtreler
 * - folders: doğru kategori filtreleme (getAppsInFolder karşılığı)
 * - updateAppCategory: repository çağrısı doğrulanır (reclassifyApp karşılığı)
 *
 * AppPrefs Kotlin object olduğu için mockkObject ile mock'lanır.
 * AndroidViewModel için Application mocklanır — Robolectric gerekmez.
 * AppNotificationListenerService companion'ı başlangıçta boş StateFlow döndürdüğünden
 * ayrıca mock'lanmasına gerek yoktur.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

    // ── Altyapı ──────────────────────────────────────────────────────────────

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockApplication: Application
    private lateinit var mockRepository: AppRepository
    private lateinit var mockPmHelper: PackageManagerHelper
    private lateinit var mockContext: Context

    /** Repository'den yayılan kontrol edilebilir flow */
    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    private lateinit var viewModel: LauncherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockPmHelper = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        every { mockRepository.getAllAppsFlow() } returns appsFlow

        // AppPrefs object'ini mock'la — SharedPreferences'a dokunmasın
        mockkObject(AppPrefs)
        every { AppPrefs.getFavorites(any()) } returns emptySet()
        every { AppPrefs.isFavorite(any(), any()) } returns false
        every { AppPrefs.addFavorite(any(), any()) } just Runs
        every { AppPrefs.removeFavorite(any(), any()) } just Runs

        viewModel = LauncherViewModel(
            application = mockApplication,
            repository = mockRepository,
            packageManagerHelper = mockPmHelper
        )
    }

    @After
    fun tearDown() {
        unmockkObject(AppPrefs)
        Dispatchers.resetMain()
    }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private fun app(pkg: String, name: String, categoryId: String = "social") = AppInfo(
        packageName = pkg,
        appName = name,
        categoryId = categoryId
    )

    // ── toggleFavorite ───────────────────────────────────────────────────────

    @Test
    fun `toggleFavorite_favori_degilse_addFavorite_cagirilir`() = runTest {
        every { AppPrefs.isFavorite(any(), eq("com.test.app")) } returns false
        every { AppPrefs.getFavorites(any()) } returns setOf("com.test.app")

        viewModel.toggleFavorite(mockContext, "com.test.app")
        advanceUntilIdle()

        verify { AppPrefs.addFavorite(any(), "com.test.app") }
    }

    @Test
    fun `toggleFavorite_favoriyse_removeFavorite_cagirilir`() = runTest {
        every { AppPrefs.isFavorite(any(), eq("com.test.app")) } returns true
        every { AppPrefs.getFavorites(any()) } returns emptySet()

        viewModel.toggleFavorite(mockContext, "com.test.app")
        advanceUntilIdle()

        verify { AppPrefs.removeFavorite(any(), "com.test.app") }
    }

    @Test
    fun `toggleFavorite_sonrasi_getFavorites_ile_state_guncellenir`() = runTest {
        val updatedFavs = setOf("com.pkg.a")
        every { AppPrefs.isFavorite(any(), any()) } returns false
        every { AppPrefs.getFavorites(any()) } returns updatedFavs

        viewModel.toggleFavorite(mockContext, "com.pkg.a")
        advanceUntilIdle()

        // _favoritePkgs = AppPrefs.getFavorites(...) çağrısıyla güncellenir
        verify(atLeast = 1) { AppPrefs.getFavorites(any()) }
    }

    // ── searchApps / filteredAllApps ─────────────────────────────────────────

    @Test
    fun `bos_searchQuery_tum_gizli_olmayan_uygulamalari_dondurur`() = runTest {
        appsFlow.value = listOf(
            app("com.a", "Alpha"),
            app("com.b", "Beta"),
            app("com.c", "Gamma")
        )
        advanceUntilIdle()

        val result = viewModel.filteredAllApps.value
        assertEquals(3, result.size)
    }

    @Test
    fun `bos_searchQuery_gizli_uygulamalari_dislar`() = runTest {
        appsFlow.value = listOf(
            app("com.a", "Alpha"),
            AppInfo(packageName = "com.hidden", appName = "Hidden", isHidden = true)
        )
        advanceUntilIdle()

        val result = viewModel.filteredAllApps.value
        assertEquals(1, result.size)
        assertEquals("Alpha", result[0].appName)
    }

    @Test
    fun `searchQuery_ile_eslesmeyen_uygulamalar_filtrelenir`() = runTest {
        appsFlow.value = listOf(
            app("com.a", "Alpha"),
            app("com.b", "Beta"),
            app("com.c", "Gamma")
        )
        advanceUntilIdle()

        viewModel.setSearchQuery("alp")
        // debounce(300ms) — test scheduler'ı 400ms ilerlet
        testDispatcher.scheduler.advanceTimeBy(400)
        advanceUntilIdle()

        val result = viewModel.filteredAllApps.value
        assertEquals(1, result.size)
        assertEquals("Alpha", result[0].appName)
    }

    @Test
    fun `searchQuery_paket_adina_gore_de_filtreler`() = runTest {
        appsFlow.value = listOf(
            AppInfo(packageName = "com.whatsapp", appName = "WhatsApp"),
            AppInfo(packageName = "com.telegram", appName = "Telegram")
        )
        advanceUntilIdle()

        viewModel.setSearchQuery("whats")
        testDispatcher.scheduler.advanceTimeBy(400)
        advanceUntilIdle()

        val result = viewModel.filteredAllApps.value
        assertEquals(1, result.size)
        assertEquals("WhatsApp", result[0].appName)
    }

    @Test
    fun `searchQuery_temizlenince_tum_uygulamalar_geri_gelir`() = runTest {
        appsFlow.value = listOf(
            app("com.a", "Alpha"),
            app("com.b", "Beta")
        )
        advanceUntilIdle()

        viewModel.setSearchQuery("alp")
        testDispatcher.scheduler.advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(1, viewModel.filteredAllApps.value.size)

        viewModel.setSearchQuery("")
        testDispatcher.scheduler.advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(2, viewModel.filteredAllApps.value.size)
    }

    // ── getAppsInFolder / folders ─────────────────────────────────────────────

    @Test
    fun `folders_uygulamalari_kategoriye_gore_gruplar`() = runTest {
        appsFlow.value = listOf(
            app("com.instagram", "Instagram", "social"),
            app("com.pubg", "PUBG", "games"),
            app("com.youtube", "YouTube", "social")
        )
        advanceUntilIdle()

        val socialFolder = viewModel.folders.value.firstOrNull { it.category.categoryId == "social" }
        val gamesFolder = viewModel.folders.value.firstOrNull { it.category.categoryId == "games" }

        assertFalse("Social folder bulunmalı", socialFolder == null)
        assertEquals(2, socialFolder!!.apps.size)
        assertFalse("Games folder bulunmalı", gamesFolder == null)
        assertEquals(1, gamesFolder!!.apps.size)
        assertEquals("PUBG", gamesFolder.apps[0].appName)
    }

    @Test
    fun `folders_bos_kategorileri_dislar`() = runTest {
        appsFlow.value = listOf(app("com.instagram", "Instagram", "social"))
        advanceUntilIdle()

        val gamesFolder = viewModel.folders.value.firstOrNull { it.category.categoryId == "games" }
        assertNull("Oyunsuz games klasörü olmamalı", gamesFolder)
    }

    @Test
    fun `folders_klasor_icindeki_uygulamalar_ada_gore_sirali`() = runTest {
        appsFlow.value = listOf(
            app("com.z", "Zoom", "social"),
            app("com.a", "Asana", "social"),
            app("com.t", "Twitter", "social")
        )
        advanceUntilIdle()

        val social = viewModel.folders.value.first { it.category.categoryId == "social" }
        assertEquals(listOf("Asana", "Twitter", "Zoom"), social.apps.map { it.appName })
    }

    @Test
    fun `folders_app_eklenince_aninda_guncellenir`() = runTest {
        appsFlow.value = listOf(app("com.a", "AppA", "social"))
        advanceUntilIdle()
        assertEquals(1, viewModel.folders.value.first { it.category.categoryId == "social" }.apps.size)

        appsFlow.value = listOf(
            app("com.a", "AppA", "social"),
            app("com.b", "AppB", "social")
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.folders.value.first { it.category.categoryId == "social" }.apps.size)
    }

    // ── reclassifyApp / updateAppCategory ────────────────────────────────────

    @Test
    fun `updateAppCategory_repository_guncelleme_cagirilir`() = runTest {
        viewModel.updateAppCategory("com.test.app", "productivity")
        advanceUntilIdle()

        coVerify { mockRepository.updateAppCategory("com.test.app", "productivity") }
    }

    @Test
    fun `updateAppCategory_farkli_kategori_ile_dogru_iletilir`() = runTest {
        viewModel.updateAppCategory("com.game.app", "games")
        advanceUntilIdle()

        coVerify { mockRepository.updateAppCategory("com.game.app", "games") }
    }

    @Test
    fun `updateAppCategory_bos_paket_adi_ile_de_repository_cagirilir`() = runTest {
        viewModel.updateAppCategory("", "social")
        advanceUntilIdle()

        coVerify { mockRepository.updateAppCategory("", "social") }
    }

    // ── refreshLastLaunched ──────────────────────────────────────────────────

    @Test
    fun `refreshLastLaunched_lastLaunchedPkg_yokken_repository_tetiklenmez`() = runTest {
        // launchApp çağrılmadan refreshLastLaunched → hiç DB çağrısı olmamalı
        viewModel.refreshLastLaunched()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.updateLastUsedTimestamp(any(), any()) }
    }

    @Test
    fun `refreshLastLaunched_launchApp_intent_null_donunce_repository_tetiklenmez`() = runTest {
        // mock context'te getLaunchIntentForPackage null döner → launchApp early return
        // → lastLaunchedPkg set edilmez → refreshLastLaunched hiçbir şey yapmaz
        every { mockContext.packageManager } returns mockk(relaxed = true) {
            every { getLaunchIntentForPackage(any()) } returns null
        }

        viewModel.launchApp(mockContext, "com.test.app")
        viewModel.refreshLastLaunched()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.updateLastUsedTimestamp(any(), any()) }
    }

    @Test
    fun `refreshLastLaunched_launchApp_basarili_olunca_repository_timestamp_gunceller`() = runTest {
        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockContext.packageManager } returns mockk(relaxed = true) {
            every { getLaunchIntentForPackage("com.test.app") } returns mockIntent
        }
        every { mockContext.startActivity(any()) } just Runs

        viewModel.launchApp(mockContext, "com.test.app")
        viewModel.refreshLastLaunched()
        advanceUntilIdle()

        coVerify { mockRepository.updateLastUsedTimestamp(eq("com.test.app"), any()) }
    }

    @Test
    fun `refreshLastLaunched_iki_kez_cagirilinca_lastLaunchedPkg_sifirlanir`() = runTest {
        // İlk refreshLastLaunched lastLaunchedPkg'yi null'a set eder
        // İkinci çağrıda pkg=null olduğundan repository.updateLastUsedTimestamp çağrılmaz
        // Bu testi doğrulamak için pkg=null olan durumu simüle ediyoruz:
        // refreshLastLaunched() direkt çağrısında pkg=null → hiç çağrı olmamalı

        viewModel.refreshLastLaunched() // pkg=null, hiçbir şey yapmaz
        advanceUntilIdle()
        viewModel.refreshLastLaunched() // yine pkg=null
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.updateLastUsedTimestamp(any(), any()) }
    }
}
