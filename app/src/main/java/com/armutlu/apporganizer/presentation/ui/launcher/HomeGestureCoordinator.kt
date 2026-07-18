package com.armutlu.apporganizer.presentation.ui.launcher

/**
 * Döngü P10 — Gesture arbitration katmanı (saf karar çekirdeği).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P10 (satır 920-986).
 *
 * AMAÇ: `HomeScreen.kt` içinde dağınık duran jest kararlarını (yatay sayfa geçişi kilidi,
 * dikey swipe-up → app drawer, dock alanı exclusion) TEK, saf ve test edilebilir bir karara
 * indirger. Bu dosya Compose/Android bağımlılığı İÇERMEZ — girdi/çıktı düz Kotlin veri
 * sınıfları ve enum'lardır, JVM unit testlerinden doğrudan çağrılabilir.
 *
 * ÖNEMLİ — MERKEZİLEŞTİRME, DAVRANIŞ DEĞİŞİKLİĞİ DEĞİL: Bu katman HomeScreen.kt'de zaten var
 * olan kuralları (bkz. dosya içi referanslar) birebir kodlar:
 * - `pagerScrollEnabled = !searchActive && !reorderActive && !modalOpen` (HomeScreen.kt ~1265)
 * - `nestedScrollConnection.onPostScroll/onPostFling` swipe-up eşiği (HomeScreen.kt ~407-428)
 * - kök `pointerInput("drag")` dikey sürükleme eşiği `-60f` (HomeScreen.kt ~592-613)
 * Yeni jest EKLENMEZ; mevcut üç karar noktası burada `decide()` çağrısına delege edilir.
 */

/** Jest arbitration'ının girdisi — anlık UI durumu. Roadmap'teki `HomeGestureContext` modeli. */
data class HomeGestureContext(
    /** Tam ekran arama (fullScreenSearchOpen) veya klasör arama sorgusu aktif mi. */
    val searchActive: Boolean = false,
    /** All Apps çekmecesi zaten açık mı (açıkken kök jest'ler devre dışı — HomeScreen ~568/577/602). */
    val allAppsOpen: Boolean = false,
    /** Dock edit, context menu, kategori seçici, klasör context menu gibi modallardan biri açık mı. */
    val modalOpen: Boolean = false,
    /** Klasör drag/reorder sürüyor mu (dragFromIndex != null). */
    val folderReorderActive: Boolean = false,
    /** Quick Wheel (radyal menü) açık mı. */
    val quickWheelOpen: Boolean = false,
    /** Dokunuş dock, arama sonuçları veya scroll alanı gibi hariç bölgede mi başladı. */
    val touchStartedInExcludedRegion: Boolean = false,
)

/** Jest arbitration'ının çıktısı — kim bu jesti alacak. Roadmap'teki `HomeGestureDecision` enum'u. */
enum class HomeGestureDecision {
    /** Yatay pager scroll'a izin verilir (HorizontalPager userScrollEnabled = true). */
    ALLOW_HORIZONTAL_PAGER,
    /** Dikey swipe-up eşiği aşıldı → App Drawer (veya kullanıcının atadığı gesture aksiyonu) açılır. */
    OPEN_ALL_APPS,
    /** Jest zaten açık bir alt bileşene (arama, modal, quick wheel, All Apps) aittir — kök karışmaz. */
    HANDLE_CHILD,
    /** Jest hiçbir hedefe atanmaz — yut (no-op). */
    IGNORE,
}

/** Kök pointerInput/nestedScroll bloklarının sorduğu jest türü. */
enum class HomeGestureKind {
    /** Yatay pager sayfa geçişi teklifi (HorizontalPager kendi iç mantığıyla sorar). */
    HORIZONTAL_DRAG,
    /** Kök Box üzerindeki dikey sürükleme (pointerInput("drag") ~592-613). */
    VERTICAL_DRAG,
    /** nestedScroll zincirinden gelen dikey scroll/fling teklifi (Dashboard iç scroll taşması). */
    NESTED_VERTICAL_SCROLL,
}

/**
 * Debug amaçlı karar nedeni — sabit enum, serbest metin YOK (telemetriye bağlanmaz, sadece
 * log/inceleme için). U02 pattern'ine uygun: sabit kod, insan tarafından okunabilir ama
 * analytics event'i değil.
 */
enum class HomeGestureReason {
    SEARCH_ACTIVE_LOCKS_ROOT,
    MODAL_OPEN_LOCKS_ROOT,
    REORDER_ACTIVE_LOCKS_ROOT,
    ALL_APPS_ALREADY_OPEN,
    QUICK_WHEEL_OPEN_LOCKS_ROOT,
    EXCLUDED_REGION_TOUCH,
    VERTICAL_SWIPE_THRESHOLD_REACHED,
    VERTICAL_SWIPE_BELOW_THRESHOLD,
    HORIZONTAL_PAGER_DEFAULT_ALLOWED,
    NESTED_SCROLL_OVERSCROLL_TRIGGERED_DRAWER,
    NESTED_SCROLL_WITHIN_BOUNDS,
}

/** [decide] çağrısının sonucu — karar + (debug için) neden. */
data class HomeGestureResult(
    val decision: HomeGestureDecision,
    val reason: HomeGestureReason,
)

/**
 * Saf jest arbitration çekirdeği. Compose tarafı ince adaptörler üzerinden bu nesneyi çağırır:
 * - `pagerScrollEnabled` hesabı → [decide] ile `HORIZONTAL_DRAG` sorgusu, sonuç `ALLOW_HORIZONTAL_PAGER` mi.
 * - kök dikey drag eşiği → [decide] ile `VERTICAL_DRAG` sorgusu + [verticalSwipeThresholdPx].
 * - nestedScroll onPostScroll/onPostFling → [decide] ile `NESTED_VERTICAL_SCROLL` sorgusu.
 *
 * Kural sırası (roadmap kural tablosu, satır 950-961 birebir):
 * 1. Search, modal, context menu veya reorder açıkken kök jest'ler kilitlenir (madde 4).
 * 2. All Apps zaten açıksa veya Quick Wheel açıksa kök jest'ler o alt bileşene aittir (HANDLE_CHILD).
 * 3. Dock, arama sonuçları, scroll alanları exclusion region'dır — kök jest'ler orada başlamaz (madde 5).
 * 4. Yatay hareket her zaman pager'a (yukarıdaki kilitler yoksa) — dikey eşikten ayrı değerlendirilir (madde 2).
 * 5. Dikey swipe eşiği yoğunluk-bağımsız (density-independent) px cinsindendir — ham `-60f` kaldırılmıştır (madde 3).
 */
object HomeGestureArbiter {

    /**
     * Varsayılan dikey swipe-up eşiği — [androidx.compose.ui.unit.Dp] cinsinden, çağıran taraf
     * `with(density) { VERTICAL_SWIPE_THRESHOLD_DP.dp.toPx() }` ile piksele çevirir. Eskiden kök
     * `pointerInput("drag")` bloğunda ham `-60f` px sabiti kullanılıyordu (density'e göre farklı
     * cihazlarda farklı fiziksel mesafe demekti) — artık tek dp sabiti burada, iki çağrı noktası
     * da (nestedScroll swipeThresholdPx zaten 80.dp idi, kök drag -60f idi) BU sabite taşınır.
     * NOT: davranış değişikliği değildir — nestedScroll tarafı zaten 80.dp kullanıyordu; kök
     * `pointerInput("drag")` bloğu da aynı mantıksal eşiğe hizalanarak tek kaynağa bağlanır.
     */
    const val VERTICAL_SWIPE_THRESHOLD_DP: Float = 60f

    /**
     * Tek karar noktası. `kind` hangi jest sorusunun sorulduğunu, `context` anlık UI durumunu,
     * `verticalDeltaPx`/`thresholdPx` yalnız [HomeGestureKind.VERTICAL_DRAG] ve
     * [HomeGestureKind.NESTED_VERTICAL_SCROLL] için anlamlıdır (kümülatif dikey mesafe, negatif =
     * yukarı sürükleme, HomeScreen.kt'deki `accumulated`/`swipeDelta` ile aynı işaret kuralı).
     */
    fun decide(
        kind: HomeGestureKind,
        context: HomeGestureContext,
        verticalDeltaPx: Float = 0f,
        thresholdPx: Float = 0f,
    ): HomeGestureResult {
        // 1) Search / modal / reorder açıkken kök hiçbir jest'i almaz (roadmap madde 4).
        if (context.searchActive) {
            return HomeGestureResult(HomeGestureDecision.IGNORE, HomeGestureReason.SEARCH_ACTIVE_LOCKS_ROOT)
        }
        if (context.modalOpen) {
            return HomeGestureResult(HomeGestureDecision.IGNORE, HomeGestureReason.MODAL_OPEN_LOCKS_ROOT)
        }
        if (context.folderReorderActive) {
            return HomeGestureResult(HomeGestureDecision.IGNORE, HomeGestureReason.REORDER_ACTIVE_LOCKS_ROOT)
        }

        // 2) All Apps veya Quick Wheel zaten açıksa jest o alt bileşene aittir — kök devre dışı.
        if (context.allAppsOpen) {
            return HomeGestureResult(HomeGestureDecision.HANDLE_CHILD, HomeGestureReason.ALL_APPS_ALREADY_OPEN)
        }
        if (context.quickWheelOpen) {
            return HomeGestureResult(HomeGestureDecision.HANDLE_CHILD, HomeGestureReason.QUICK_WHEEL_OPEN_LOCKS_ROOT)
        }

        // 3) Dock/arama sonuçları/scroll alanı gibi exclusion region'da başlayan dokunuş köke ait değildir.
        if (context.touchStartedInExcludedRegion) {
            return HomeGestureResult(HomeGestureDecision.HANDLE_CHILD, HomeGestureReason.EXCLUDED_REGION_TOUCH)
        }

        return when (kind) {
            // 4) Yatay hareket — yukarıdaki kilitlerden hiçbiri tetiklenmediyse pager serbest.
            HomeGestureKind.HORIZONTAL_DRAG ->
                HomeGestureResult(HomeGestureDecision.ALLOW_HORIZONTAL_PAGER, HomeGestureReason.HORIZONTAL_PAGER_DEFAULT_ALLOWED)

            // 5) Kök dikey sürükleme — density-bağımsız eşik aşıldıysa App Drawer açılır.
            HomeGestureKind.VERTICAL_DRAG ->
                if (verticalDeltaPx < -thresholdPx) {
                    HomeGestureResult(HomeGestureDecision.OPEN_ALL_APPS, HomeGestureReason.VERTICAL_SWIPE_THRESHOLD_REACHED)
                } else {
                    HomeGestureResult(HomeGestureDecision.IGNORE, HomeGestureReason.VERTICAL_SWIPE_BELOW_THRESHOLD)
                }

            // 6) Dashboard içi dikey scroll taşması (nestedScroll onPostScroll/onPostFling) —
            // Dashboard kendi içeriğini tükettikten SONRA kalan miktar köke devredilir; eşik
            // aşıldıysa App Drawer açılır, aşılmadıysa jest Dashboard'ın kendi scroll'unda kalır.
            HomeGestureKind.NESTED_VERTICAL_SCROLL ->
                if (verticalDeltaPx < -thresholdPx) {
                    HomeGestureResult(HomeGestureDecision.OPEN_ALL_APPS, HomeGestureReason.NESTED_SCROLL_OVERSCROLL_TRIGGERED_DRAWER)
                } else {
                    HomeGestureResult(HomeGestureDecision.HANDLE_CHILD, HomeGestureReason.NESTED_SCROLL_WITHIN_BOUNDS)
                }
        }
    }

    /**
     * İnce adaptör — `HomePagerHost`'a verilecek `userScrollEnabled` değerini üretir. HomeScreen.kt
     * ~1265'teki `pagerScrollEnabled = !searchActive && !reorderActive && !modalOpen` satırının
     * birebir karşılığıdır (allAppsOpen/quickWheelOpen o satırda zaten kontrol edilmiyordu —
     * pager slotu allAppsOpen==true iken de arkada var olmaya devam eder, bu yüzden burada da
     * yalnız search/modal/reorder kilidi uygulanır — davranış DEĞİŞMEZ).
     */
    fun isHorizontalPagerScrollEnabled(context: HomeGestureContext): Boolean {
        val contextWithoutAllAppsGate = context.copy(allAppsOpen = false, quickWheelOpen = false)
        return decide(HomeGestureKind.HORIZONTAL_DRAG, contextWithoutAllAppsGate).decision ==
            HomeGestureDecision.ALLOW_HORIZONTAL_PAGER
    }
}
