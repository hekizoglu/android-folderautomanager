package com.armutlu.apporganizer.presentation.ui.launcher

/**
 * Döngü P20 — Telefon/tablet adaptif düzen tek doğruluk kaynağı. Öncesinde `HomeScreen.kt`
 * içinde dağınık üç eşik vardı (isTablet 600dp, screenColumns 600/840dp, AllAppsDrawer side
 * panel sabit 380dp) — bu politika onları TEK bir `HomeDeviceClass` sınıflandırmasından türetir.
 *
 * Compose/Android bağımlılığı yoktur (JVM birim testinden doğrudan çağrılabilir) — mevcut
 * `HomeLayoutMath` pattern'iyle aynı. `material3-window-size-class`
 * bağımlılığı YOK — proje zaten `LocalConfiguration.screenWidthDp` okuyor, bu politika o değeri
 * girdi olarak alır (roadmap P20 "Nasıl yapılmalı" madde 1: mevcut `screenWidthDp` yaklaşımı tek
 * helper'a alınır, yeni bağımlılık eklenmez).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P20 (satır 1432-1500).
 */
object HomeAdaptiveLayoutPolicy {

    /** Küçük tablet alt sınırı — mevcut `isTablet`/`screenColumns` 600dp eşiğiyle birebir aynı. */
    private const val COMPACT_TABLET_MIN_WIDTH_DP = 600

    /** Büyük tablet alt sınırı — mevcut `screenColumns` 840dp eşiğiyle birebir aynı. */
    private const val EXPANDED_TABLET_MIN_WIDTH_DP = 840

    /** Telefon ve küçük tablette All Apps side panel kullanılmaz (telefonda tam ekran overlay). */
    const val ALL_APPS_SIDE_PANEL_WIDTH_COMPACT_DP = 380

    /** Büyük tablette side panel biraz daha geniş — roadmap: "Global search maksimum genişlik
     * alır; tüm ekranı gereksiz uzatmaz" ilkesiyle aynı orana göre büyütülür ama sabit bir tavanı
     * vardır (ekranın tamamını kaplamaz). */
    const val ALL_APPS_SIDE_PANEL_WIDTH_EXPANDED_DP = 420

    /** Büyük tablette arama çubuğu/dock bu genişlikten fazla büyümez — roadmap P20 "Büyük
     * tablet" bölümü madde 2-3: "Global search maksimum genişlik alır", "Dock maksimum
     * genişlikle ortalanır". Küçük tablet ve telefonda `fillMaxWidth()` davranışı korunur (bu
     * sabit yalnız EXPANDED_TABLET için `widthIn(max=...)` olarak uygulanır). */
    const val EXPANDED_CONTENT_MAX_WIDTH_DP = 720

    /**
     * Ekran genişliğine göre cihaz sınıfı — Dashboard yoğunluğu, klasör sütun sayısı, All Apps
     * panel genişliği ve içerik maksimum genişliği HEPSİ bu tek fonksiyondan türetilir.
     */
    fun deviceClass(screenWidthDp: Int): HomeDeviceClass = when {
        screenWidthDp >= EXPANDED_TABLET_MIN_WIDTH_DP -> HomeDeviceClass.EXPANDED_TABLET
        screenWidthDp >= COMPACT_TABLET_MIN_WIDTH_DP -> HomeDeviceClass.COMPACT_TABLET
        else -> HomeDeviceClass.PHONE
    }

    /**
     * Klasör grid sütun sayısı — eskiden `HomeScreen.kt` içindeki inline `when` ve
     * `HomeLayoutMath.screenColumns` ikisi ayrı ayrı aynı eşikleri tekrarlıyordu; artık ikisi de
     * bu fonksiyona (veya `deviceClass`'a) delege eder. Telefon 4, küçük tablet 5, büyük tablet 6
     * sütun (roadmap P20 "Telefon"/"Küçük tablet"/"Büyük tablet" bölümleri).
     */
    fun folderColumns(deviceClass: HomeDeviceClass): Int = when (deviceClass) {
        HomeDeviceClass.PHONE -> 4
        HomeDeviceClass.COMPACT_TABLET -> 5
        HomeDeviceClass.EXPANDED_TABLET -> 6
    }

    /**
     * All Apps drawer tablette sağ side panel olarak açılır (P11), telefonda tam ekran overlay
     * (bu fonksiyon telefon için çağrılmaz — `HomeScreen.kt` `isTablet` kontrolüyle sarmalar).
     */
    fun allAppsSidePanelWidthDp(deviceClass: HomeDeviceClass): Int = when (deviceClass) {
        HomeDeviceClass.PHONE -> ALL_APPS_SIDE_PANEL_WIDTH_COMPACT_DP
        HomeDeviceClass.COMPACT_TABLET -> ALL_APPS_SIDE_PANEL_WIDTH_COMPACT_DP
        HomeDeviceClass.EXPANDED_TABLET -> ALL_APPS_SIDE_PANEL_WIDTH_EXPANDED_DP
    }

    /**
     * Arama çubuğu ve dock için maksimum genişlik — yalnız EXPANDED_TABLET'te tavan uygulanır
     * (roadmap: "tüm ekranı gereksiz uzatmaz", "Dock maksimum genişlikle ortalanır"). Telefon ve
     * küçük tablette `null` döner — çağıran yer `fillMaxWidth()` davranışını değiştirmeden korur.
     */
    fun centeredContentMaxWidthDp(deviceClass: HomeDeviceClass): Int? = when (deviceClass) {
        HomeDeviceClass.EXPANDED_TABLET -> EXPANDED_CONTENT_MAX_WIDTH_DP
        else -> null
    }

    /** `isTablet` eski davranışıyla birebir uyumlu köprü — side panel/side-panel scrim kararı
     * hâlâ "tablet mi değil mi" ikili sorusuna dayanıyor (COMPACT_TABLET + EXPANDED_TABLET). */
    fun isTablet(deviceClass: HomeDeviceClass): Boolean = deviceClass != HomeDeviceClass.PHONE
}

/**
 * Döngü P20 — telefon/küçük tablet/büyük tablet sınıflandırması. Hero yüksekliği kendi adaptif
 * profilinden, bu sınıf ise genişlik/cihaz kararlarından sorumludur.
 */
enum class HomeDeviceClass { PHONE, COMPACT_TABLET, EXPANDED_TABLET }
