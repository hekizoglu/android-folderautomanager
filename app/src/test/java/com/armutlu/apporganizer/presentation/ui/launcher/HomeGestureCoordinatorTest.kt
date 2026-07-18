package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P10 — `HomeGestureArbiter` saf jest arbitration çekirdeğinin birim testleri.
 * Compose/Android bağımlılığı yoktur — roadmap'in kural tablosunun (satır 950-961, 969-977)
 * TÜM satırları burada karşılık bulur.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P10.
 */
class HomeGestureCoordinatorTest {

    private val threshold = 60f

    // ── 1) Search açıkken kök jest'ler kilitlenir (madde 4 / test: "Search açıkken All Apps açılmaz") ──

    @Test fun `search aktifken dikey swipe IGNORE doner`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(searchActive = true),
            verticalDeltaPx = -200f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.IGNORE, result.decision)
        assertEquals(HomeGestureReason.SEARCH_ACTIVE_LOCKS_ROOT, result.reason)
    }

    @Test fun `search aktifken yatay pager de kilitli (ALLOW degil)`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.HORIZONTAL_DRAG,
            context = HomeGestureContext(searchActive = true),
        )
        assertEquals(HomeGestureDecision.IGNORE, result.decision)
    }

    @Test fun `isHorizontalPagerScrollEnabled search aktifken false doner`() {
        assertEquals(
            false,
            HomeGestureArbiter.isHorizontalPagerScrollEnabled(HomeGestureContext(searchActive = true))
        )
    }

    // ── 2) Dashboard içi dikeyken yatay pager çalışır (bağımsız eksenler) ──────────────────────

    @Test fun `dashboard icinde dikey scroll surerken yatay pager hala serbest`() {
        // Dikey nestedScroll eşik altında kalırsa (Dashboard kendi içinde tüketiyor) — bu, kökün
        // yatay pager kararını ETKİLEMEZ; iki eksen bağımsız değerlendirilir.
        val vertical = HomeGestureArbiter.decide(
            kind = HomeGestureKind.NESTED_VERTICAL_SCROLL,
            context = HomeGestureContext(),
            verticalDeltaPx = -10f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.HANDLE_CHILD, vertical.decision)

        val horizontal = HomeGestureArbiter.decide(
            kind = HomeGestureKind.HORIZONTAL_DRAG,
            context = HomeGestureContext(),
        )
        assertEquals(HomeGestureDecision.ALLOW_HORIZONTAL_PAGER, horizontal.decision)
    }

    // ── 3) En altta iken yukarı sürükleme OPEN_ALL_APPS (dashboard taşması + kök drag) ─────────

    @Test fun `nested scroll esik asilinca OPEN_ALL_APPS doner`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.NESTED_VERTICAL_SCROLL,
            context = HomeGestureContext(),
            verticalDeltaPx = -90f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.OPEN_ALL_APPS, result.decision)
        assertEquals(HomeGestureReason.NESTED_SCROLL_OVERSCROLL_TRIGGERED_DRAWER, result.reason)
    }

    @Test fun `nested scroll esik altinda HANDLE_CHILD doner (dashboard kendi scrollunda kalir)`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.NESTED_VERTICAL_SCROLL,
            context = HomeGestureContext(),
            verticalDeltaPx = -40f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.HANDLE_CHILD, result.decision)
        assertEquals(HomeGestureReason.NESTED_SCROLL_WITHIN_BOUNDS, result.reason)
    }

    @Test fun `kok dikey surukleme esik asilinca OPEN_ALL_APPS doner`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(),
            verticalDeltaPx = -61f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.OPEN_ALL_APPS, result.decision)
        assertEquals(HomeGestureReason.VERTICAL_SWIPE_THRESHOLD_REACHED, result.reason)
    }

    @Test fun `kok dikey surukleme esik altinda IGNORE doner`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(),
            verticalDeltaPx = -59f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.IGNORE, result.decision)
        assertEquals(HomeGestureReason.VERTICAL_SWIPE_BELOW_THRESHOLD, result.reason)
    }

    // ── 4) Diagonal gesture yalnız bir karar üretir — aynı context+kind çağrısı deterministiktir ─

    @Test fun `ayni girdi ile decide her zaman ayni karari uretir (deterministik tek karar)`() {
        val context = HomeGestureContext()
        val first = HomeGestureArbiter.decide(HomeGestureKind.VERTICAL_DRAG, context, -100f, threshold)
        val second = HomeGestureArbiter.decide(HomeGestureKind.VERTICAL_DRAG, context, -100f, threshold)
        assertEquals(first, second)
    }

    // ── 5) Folder drag sırasında page değişmez (reorder → pager kilidi) ────────────────────────

    @Test fun `reorder aktifken yatay pager kilitli`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.HORIZONTAL_DRAG,
            context = HomeGestureContext(folderReorderActive = true),
        )
        assertEquals(HomeGestureDecision.IGNORE, result.decision)
        assertEquals(HomeGestureReason.REORDER_ACTIVE_LOCKS_ROOT, result.reason)
    }

    @Test fun `isHorizontalPagerScrollEnabled reorder aktifken false doner`() {
        assertEquals(
            false,
            HomeGestureArbiter.isHorizontalPagerScrollEnabled(HomeGestureContext(folderReorderActive = true))
        )
    }

    @Test fun `reorder aktifken dikey swipe de kilitli`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(folderReorderActive = true),
            verticalDeltaPx = -200f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.IGNORE, result.decision)
    }

    // ── 6) Modal/dock edit açıkken kök jest'ler kilitlenir ──────────────────────────────────────

    @Test fun `modal acikken yatay pager kilitli`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.HORIZONTAL_DRAG,
            context = HomeGestureContext(modalOpen = true),
        )
        assertEquals(HomeGestureDecision.IGNORE, result.decision)
        assertEquals(HomeGestureReason.MODAL_OPEN_LOCKS_ROOT, result.reason)
    }

    @Test fun `isHorizontalPagerScrollEnabled modal acikken false doner`() {
        assertEquals(
            false,
            HomeGestureArbiter.isHorizontalPagerScrollEnabled(HomeGestureContext(modalOpen = true))
        )
    }

    // ── 7) Dock üzerinde swipe uygulama çekmecesini yanlış açmaz (exclusion region) ─────────────

    @Test fun `exclusion bolgede baslayan dikey surukleme HANDLE_CHILD doner (kok almaz)`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(touchStartedInExcludedRegion = true),
            verticalDeltaPx = -200f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.HANDLE_CHILD, result.decision)
        assertEquals(HomeGestureReason.EXCLUDED_REGION_TOUCH, result.reason)
    }

    // ── 8) All Apps zaten açıkken kök jest'ler devre dışı (drawer kendi jestini yönetir) ────────

    @Test fun `all apps acikken kok dikey surukleme HANDLE_CHILD doner`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(allAppsOpen = true),
            verticalDeltaPx = -200f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.HANDLE_CHILD, result.decision)
        assertEquals(HomeGestureReason.ALL_APPS_ALREADY_OPEN, result.reason)
    }

    @Test fun `all apps acikken yatay pager de kok tarafindan alinmaz`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.HORIZONTAL_DRAG,
            context = HomeGestureContext(allAppsOpen = true),
        )
        assertEquals(HomeGestureDecision.HANDLE_CHILD, result.decision)
    }

    @Test fun `isHorizontalPagerScrollEnabled allAppsOpen iken false doner (P11 madde 7)`() {
        // Döngü P11 (roadmap madde 7): "All Apps open olduğunda root pager userScrollEnabled = false".
        // P10'da bilerek gate edilmiyordu (pager arkada var olmaya devam ediyordu); P11 bunu
        // kapatıyor — drawer açıkken kök pager parmak izini almamalı.
        assertEquals(
            false,
            HomeGestureArbiter.isHorizontalPagerScrollEnabled(HomeGestureContext(allAppsOpen = true))
        )
    }

    @Test fun `isHorizontalPagerScrollEnabled quickWheelOpen iken false doner (P11 madde 7)`() {
        assertEquals(
            false,
            HomeGestureArbiter.isHorizontalPagerScrollEnabled(HomeGestureContext(quickWheelOpen = true))
        )
    }

    // ── 9) Quick Wheel açıkken kök jest'ler devre dışı ──────────────────────────────────────────

    @Test fun `quick wheel acikken kok dikey surukleme HANDLE_CHILD doner`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.VERTICAL_DRAG,
            context = HomeGestureContext(quickWheelOpen = true),
            verticalDeltaPx = -200f,
            thresholdPx = threshold,
        )
        assertEquals(HomeGestureDecision.HANDLE_CHILD, result.decision)
        assertEquals(HomeGestureReason.QUICK_WHEEL_OPEN_LOCKS_ROOT, result.reason)
    }

    // ── 10) Kilit önceliği: search > modal > reorder > allApps > quickWheel > exclusion ─────────

    @Test fun `birden fazla kilit aktifse search en yuksek onceliklidir`() {
        val result = HomeGestureArbiter.decide(
            kind = HomeGestureKind.HORIZONTAL_DRAG,
            context = HomeGestureContext(
                searchActive = true,
                modalOpen = true,
                folderReorderActive = true,
                allAppsOpen = true,
                quickWheelOpen = true,
            ),
        )
        assertEquals(HomeGestureReason.SEARCH_ACTIVE_LOCKS_ROOT, result.reason)
    }

    // ── 11) Varsayılan eşik sabiti tek kaynaktır (density-bağımsız, roadmap madde 3) ────────────

    @Test fun `VERTICAL_SWIPE_THRESHOLD_DP 60 dp sabiti tek kaynaktir`() {
        assertEquals(60f, HomeGestureArbiter.VERTICAL_SWIPE_THRESHOLD_DP)
    }
}
