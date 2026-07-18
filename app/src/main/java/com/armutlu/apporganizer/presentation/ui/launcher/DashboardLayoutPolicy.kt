package com.armutlu.apporganizer.presentation.ui.launcher

/**
 * Döngü P07 — Dashboard'un dikey alan yoğunluğunu (kart boyutları/boşluklar) belirleyen saf
 * politika. `SmartDashboardPage` bu sonuca göre kompakt varyantlar seçer; Compose/Android
 * bağımlılığı yoktur, doğrudan birim testlerinden çağrılabilir (roadmap P06 madde 5, P07 madde 3).
 *
 * Amaç: Dashboard mümkün olduğunca dikey kaydırmasız (scroll'suz) tam ekrana sığmalıdır (P07 ürün
 * kararı, satır 761) — global swipe-up (app drawer) her sayfada çalışacağını kullanıcı bekler.
 * `mode()` içerik yoğunluğuna göre COMPACT/ULTRA_COMPACT seçerek scroll ihtiyacını azaltır; yine
 * de içerik sığmazsa `SmartDashboardPage`'in `verticalScroll`u devrede kalır ve taşan kısım nested
 * scroll zinciriyle (Compose'un doğal parent-scroll teslimi) `HomeScreen`'in swipe-up
 * `NestedScrollConnection`'ına akar — ayrı bir gesture-arbitration mekanizması GEREKMEZ, Compose
 * `verticalScroll` zaten kendi tükettiğini `consumed`, kalanı `available` olarak üst NestedScroll
 * zincirine iletir (bkz. `HomeScreen.kt` `nestedScrollConnection`, satır ~402-423).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P07 (satır 757-804),
 * Döngü P06 madde 5 (satır 711).
 */
object DashboardLayoutPolicy {

    /** Kompakt eşik altı ekran yüksekliği (dp) — Nexus 4/küçük telefonlar. */
    private const val COMPACT_HEIGHT_THRESHOLD_DP = 700

    /** Ultra kompakt eşik altı ekran yüksekliği (dp) — 640dp altı roadmap testi. */
    private const val ULTRA_COMPACT_HEIGHT_THRESHOLD_DP = 640

    /** Bu sayıdan fazla görünür section varsa yoğunluk bir kademe artırılır. */
    private const val HIGH_SECTION_COUNT_THRESHOLD = 5

    /** Widget alanı açıkken kullanılabilir alan daha kıymetlidir — eşik düşürülür. */
    private const val WIDGET_HIGH_SECTION_COUNT_THRESHOLD = 4

    /**
     * Kullanılabilir Dashboard yüksekliği (arama çubuğu/indicator/dock zaten `HomeShell`
     * seviyesinde çıkarılmıştır — bkz. `HomeScreen.kt` `BoxWithConstraints`), görünür section
     * sayısı ve widget alanının açık olup olmadığına göre yoğunluk modu döner.
     */
    fun mode(
        screenHeightDp: Int,
        visibleSectionCount: Int,
        hasWidgets: Boolean,
    ): DashboardDensity {
        val sectionThreshold = if (hasWidgets) {
            WIDGET_HIGH_SECTION_COUNT_THRESHOLD
        } else {
            HIGH_SECTION_COUNT_THRESHOLD
        }
        val manySection = visibleSectionCount >= sectionThreshold

        return when {
            screenHeightDp < ULTRA_COMPACT_HEIGHT_THRESHOLD_DP -> DashboardDensity.ULTRA_COMPACT
            screenHeightDp < COMPACT_HEIGHT_THRESHOLD_DP -> DashboardDensity.COMPACT
            manySection -> DashboardDensity.COMPACT
            else -> DashboardDensity.COMFORTABLE
        }
    }
}

/**
 * Dashboard section yoğunluk seviyesi — `SmartDashboardPage` bu değere göre kart iç boşluklarını
 * (padding/spacing) daraltır. Görsel uygulama `SmartDashboardPage.kt`'de yapılır; bu enum saf
 * karar sonucunu taşır (Compose bağımlılığı yok).
 */
enum class DashboardDensity { COMFORTABLE, COMPACT, ULTRA_COMPACT }
