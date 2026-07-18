package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P12 — [resolveHomeCommand] saf karar çekirdeği testleri.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P12 (satır 1020-1073).
 * Bu test yalnızca `allAppsOpen=false` senaryosunu kapsar — `allAppsOpen=true` durumu
 * LauncherActivity.onNewIntent() tarafından [resolveHomeCommand] hiç çağrılmadan ele alınır
 * (bkz. LauncherHomePressPolicyTest + HomeCommandPolicy.kt dosya başı notu).
 */
class HomeCommandResolverTest {

    @Test
    fun `search acikken Home CloseSearch dondurur, basis zamanini degistirmez`() {
        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = true, modalOpen = false),
            lastHomePressMs = 5_000L,
            nowMs = 10_000L,
        )

        assertEquals(HomeCommand.CloseSearch, result.command)
        assertEquals(5_000L, result.nextLastHomePressMs)
    }

    @Test
    fun `modal acikken Home CloseModal dondurur, basis zamanini degistirmez`() {
        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = false, modalOpen = true),
            lastHomePressMs = 5_000L,
            nowMs = 10_000L,
        )

        assertEquals(HomeCommand.CloseModal, result.command)
        assertEquals(5_000L, result.nextLastHomePressMs)
    }

    @Test
    fun `search ve modal ayni anda acikken search onceliklidir`() {
        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = true, modalOpen = true),
            lastHomePressMs = 0L,
            nowMs = 1_000L,
        )

        assertEquals(HomeCommand.CloseSearch, result.command)
    }

    @Test
    fun `search ve modal kapaliyken ilk Home GoToStartPage dondurur ve basis zamanini kaydeder`() {
        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = false, modalOpen = false),
            lastHomePressMs = 0L,
            nowMs = 10_000L,
        )

        assertEquals(HomeCommand.GoToStartPage, result.command)
        assertEquals(10_000L, result.nextLastHomePressMs)
    }

    @Test
    fun `500ms icinde ikinci Home OpenAllApps dondurur ve basis zamanini sifirlar`() {
        val first = 10_000L
        val second = first + HOME_COMMAND_DOUBLE_PRESS_WINDOW_MS

        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = false, modalOpen = false),
            lastHomePressMs = first,
            nowMs = second,
        )

        assertEquals(HomeCommand.OpenAllApps, result.command)
        assertEquals(0L, result.nextLastHomePressMs)
    }

    @Test
    fun `500ms sonra gelen Home yeniden GoToStartPage sayilir`() {
        val first = 10_000L
        val late = first + HOME_COMMAND_DOUBLE_PRESS_WINDOW_MS + 1

        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = false, modalOpen = false),
            lastHomePressMs = first,
            nowMs = late,
        )

        assertEquals(HomeCommand.GoToStartPage, result.command)
        assertEquals(late, result.nextLastHomePressMs)
    }

    @Test
    fun `search acikken ikinci basis penceresinde olsa bile CloseSearch onceliklidir`() {
        // Search açıkken çift-basış penceresi tamamen devre dışı kalır — kullanıcı Home'a art
        // arda basıyor olsa bile search kapanana kadar All Apps AÇILMAZ (roadmap madde 2 önceliği).
        val first = 10_000L
        val second = first + 100L

        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = true, modalOpen = false),
            lastHomePressMs = first,
            nowMs = second,
        )

        assertEquals(HomeCommand.CloseSearch, result.command)
        assertEquals(first, result.nextLastHomePressMs)
    }

    @Test
    fun `tam sinirda 500ms fark hala cift basis sayilir`() {
        val result = resolveHomeCommand(
            context = HomeCommandContext(searchActive = false, modalOpen = false),
            lastHomePressMs = 0L,
            nowMs = HOME_COMMAND_DOUBLE_PRESS_WINDOW_MS,
        )

        assertTrue(result.command is HomeCommand.OpenAllApps)
    }
}
