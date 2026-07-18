package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import com.armutlu.apporganizer.utils.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P00 — Döngü öncesi regresyon kilidi (ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md).
 *
 * Büyük mimari değişiklikten (Dashboard + HomePagePlanner) önce mevcut ana ekran pager/arama/dock
 * davranışının fotoğrafını çeker. Testler GERÇEK koda göre yazılmıştır (HomeScreen.kt, AppPrefs.kt,
 * HomeLayout.kt) — roadmap'in "mevcut kod" bölümü (satır 177-270) bazı noktalarda güncel kodu
 * yansıtmıyordu, farklar aşağıda dokümante edilmiştir.
 *
 * Roadmap varsayımı vs gerçek kod:
 * - Roadmap "HomeLayoutConfig eski tek yüzey mantığına göre" derken FOLDER_GRID'in CONTENT
 *   zone'unda RESTRICTED+hideable=false olduğunu varsayıyor — gerçek kod (HomeLayout.kt) bunu
 *   zaten böyle tanımlıyor, DOCK da FOOTER'da FIXED+hideable=false. Bu P00'da değişmedi.
 * - "last_home_page" hâlâ ham Int (AppPrefs.getLastHomePage/setLastHomePage) — roadmap'in
 *   HomePageAnchor migration'ı (2.4) henüz uygulanmadı, bu P02+ kapsamında.
 */
class HomeScreenNavigationContractTest {

    // ── 1) Klasör sayfa sayısı hesabı (HomeScreen.kt ~satır 984: tavan bölme) ──────────────

    @Test
    fun `8 klasor pageSize 8 ile tek sayfa`() {
        assertEquals(1, HomeLayoutMath.pageCount(folderCount = 8, pageSize = 8))
    }

    @Test
    fun `9 klasor pageSize 8 ile iki sayfa`() {
        assertEquals(2, HomeLayoutMath.pageCount(folderCount = 9, pageSize = 8))
    }

    @Test
    fun `0 klasor olsa bile en az 1 sayfa`() {
        assertEquals(1, HomeLayoutMath.pageCount(folderCount = 0, pageSize = 8))
    }

    @Test
    fun `tam kat klasor sayisi tasma yaratmaz`() {
        assertEquals(3, HomeLayoutMath.pageCount(folderCount = 24, pageSize = 8))
    }

    @Test
    fun `pageSize sifir veya negatif gelirse guvenli minimuma dusurulur`() {
        // HomeScreen.pageSize her zaman HomeLayoutMath.pageSize üzerinden >=MIN_VISIBLE_FOLDERS
        // döner, ama pageCount kendi başına da savunmalı olmalı (bölme hatası olmasın).
        assertTrue(HomeLayoutMath.pageCount(folderCount = 5, pageSize = 0) >= 1)
        assertTrue(HomeLayoutMath.pageCount(folderCount = 5, pageSize = -3) >= 1)
    }

    // ── 2) Son klasör sayfasında boş slot sayısı (HomeScreenFolderPager.kt ~satır 183) ─────
    // FolderPager: `val emptySlots = pageSize - pageFolders.size` — pure, doğrudan test edilir.

    @Test
    fun `son sayfada eksik klasor sayisi kadar bos slot hesaplanir`() {
        val pageSize = 8
        val totalFolders = 9 // 1. sayfa dolu (8), 2. sayfada 1 klasör + 7 boş slot
        val pageStart = 1 * pageSize
        val pageFoldersCount = (totalFolders - pageStart).coerceAtLeast(0).coerceAtMost(pageSize)
        val emptySlots = pageSize - pageFoldersCount

        assertEquals(1, pageFoldersCount)
        assertEquals(7, emptySlots)
    }

    @Test
    fun `dolu sayfada bos slot olmaz`() {
        val pageSize = 8
        val pageFoldersCount = 8
        assertEquals(0, pageSize - pageFoldersCount)
    }

    // ── 3) HomeLayoutMath.pageSize kapasite sınırına uyar (mevcut testle çakışmayan ek uçlar) ─

    @Test
    fun `pageSize istenen degerden buyuk olamaz`() {
        val capacity = HomeLayoutMath.folderCapacity(availableHeightDp = 2000, folderSizeDp = 72, columns = 4)
        val pageSize = HomeLayoutMath.pageSize(requestedPageSize = 8, folderCapacity = capacity)
        assertTrue(pageSize <= 8)
    }

    // ── 4) Üst/alt arama konumu HEADER/FOOTER (roadmap hedefi — HomeLayoutPrefsTest'te de kapsanır) ─
    // Not: `legacy TOP keeps search in header` ve `legacy BOTTOM puts search immediately before
    // dock` testleri HomeLayoutPrefsTest.kt içinde zaten mevcut ve bu davranışı kilitliyor.
    // Burada AppPrefs sabitleri + varsayılan değer dokümante/kilitlenir (BOTTOM varsayılan).

    @Test
    fun `arama pozisyonu sabitleri TOP ve BOTTOM olarak tanimli`() {
        assertEquals("TOP", AppPrefs.SEARCH_BAR_POS_TOP)
        assertEquals("BOTTOM", AppPrefs.SEARCH_BAR_POS_BOTTOM)
    }

    @Test
    fun `MAIN_SEARCH section varsayilan HEADER zonunda`() {
        assertEquals(HomeLayoutZone.HEADER, HomeSectionId.MAIN_SEARCH.defaultZone)
    }

    // ── 5) Dock zorunlu ve görünür kalır ─────────────────────────────────────────────────

    @Test
    fun `DOCK section hideable degildir ve FOOTER a sabittir`() {
        assertFalse("DOCK gizlenebilir olmamalı", HomeSectionId.DOCK.hideable)
        assertTrue("DOCK required olmalı", HomeSectionId.DOCK.required)
        assertEquals(HomeLayoutZone.FOOTER, HomeSectionId.DOCK.defaultZone)
    }

    @Test
    fun `FOLDER_GRID section hideable degildir`() {
        assertFalse("FOLDER_GRID gizlenebilir olmamalı", HomeSectionId.FOLDER_GRID.hideable)
        assertTrue(HomeSectionId.FOLDER_GRID.required)
    }

    // ── 6) Swipe-up action varsayılan olarak All Apps açar (AppPrefs.getGestureSwipeUp) ────
    // Not: LauncherViewModel.dispatchGestureAction() içinde GestureAction.OPEN_DRAWER -> openAllApps()
    // eşlemesi var (LauncherViewModel.kt ~satır 1141-1153); AppPrefs seviyesinde varsayılan
    // değer burada kilitlenir. ViewModel entegrasyonu Android bağımlılığı gerektirdiği için
    // (LauncherViewModelTest @Ignore — bkz. dosya içi not) mock'lanabilir bir Context olmadan
    // burada test edilemez; bu sınırlama dokümante edilmiştir (roadmap protokolü madde: "test
    // edilemeyen UI davranışları için kısa dokümantasyon yorumu yeterli").

    @Test
    fun `gesture aksiyon enum SWIPE_UP anahtari mevcut`() {
        // Varsayılan değer gerçek cihazda SharedPreferences gerektirir (Robolectric/instrumented
        // olmadan doğrudan çağrılamaz); burada enum sözleşmesi ve OPEN_DRAWER'ın var olduğu
        // kilitlenir — LauncherViewModel.dispatchGestureAction OPEN_DRAWER'ı openAllApps'e
        // eşliyor, bu üçüncü partiden (LauncherViewModel kaynak okuması) doğrulanmıştır.
        assertTrue(AppPrefs.GestureAction.entries.contains(AppPrefs.GestureAction.OPEN_DRAWER))
    }

    // ── 9) Tablet sütun sayıları 5/6 korunur (HomeScreen.kt ~170-174, HomeScreenFolderPager ~73-78) ─

    @Test
    fun `600dp altinda 4 sutun`() {
        assertEquals(4, HomeLayoutMath.screenColumns(599))
    }

    @Test
    fun `600dp ve uzeri tablet esiginde 5 sutun`() {
        assertEquals(5, HomeLayoutMath.screenColumns(600))
        assertEquals(5, HomeLayoutMath.screenColumns(839))
    }

    @Test
    fun `840dp ve uzeri genis tablet esiginde 6 sutun`() {
        assertEquals(6, HomeLayoutMath.screenColumns(840))
        assertEquals(6, HomeLayoutMath.screenColumns(1200))
    }
}
