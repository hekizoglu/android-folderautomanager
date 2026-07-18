package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P00 — Döngü öncesi regresyon kilidi.
 *
 * LauncherActivity.onNewIntent() Home tuşuna basılınca (All Apps kapalıyken) şu politikayı
 * uygular: ≤500ms içinde ikinci basış gelirse All Apps açılır, aksi halde basış zamanı
 * kaydedilir. Bu davranış [homePressDecision] pure fonksiyonuna çıkarılmıştır — test
 * edilebilirlik için minimum görünürlük değişikliği (LauncherActivity.onNewIntent artık bu
 * fonksiyonu çağırıyor, karar mantığı aynı kalıyor).
 *
 * All Apps zaten açıkken bu fonksiyon hiç çağrılmaz — LauncherActivity.onNewIntent önce
 * viewModel.allAppsOpen kontrolü yapıp erken döner (bkz. roadmap P00 test hedefi #7,
 * HomeScreenNavigationContractTest içinde dokümante edilmiştir).
 */
class LauncherHomePressPolicyTest {

    @Test
    fun `ilk home basisi All Apps acmaz, basis zamanini kaydeder`() {
        val decision = homePressDecision(lastHomePressMs = 0L, nowMs = 10_000L)

        assertTrue(decision is HomePressDecision.RecordPress)
        assertEquals(10_000L, decision.nextLastHomePressMs)
    }

    @Test
    fun `500ms icinde ikinci basis All Apps acar`() {
        val first = 10_000L
        val second = first + HOME_DOUBLE_PRESS_WINDOW_MS

        val decision = homePressDecision(lastHomePressMs = first, nowMs = second)

        assertTrue(decision is HomePressDecision.OpenAllApps)
    }

    @Test
    fun `tam sinirda 500ms fark hala cift basis sayilir`() {
        val decision = homePressDecision(lastHomePressMs = 0L, nowMs = HOME_DOUBLE_PRESS_WINDOW_MS)

        assertTrue("500ms sınırı dahil olmalı (<=)", decision is HomePressDecision.OpenAllApps)
    }

    @Test
    fun `500ms sonra gelen basis tek basis sayilir ve yeniden kaydedilir`() {
        val first = 10_000L
        val late = first + HOME_DOUBLE_PRESS_WINDOW_MS + 1

        val decision = homePressDecision(lastHomePressMs = first, nowMs = late)

        assertTrue(decision is HomePressDecision.RecordPress)
        assertEquals(late, decision.nextLastHomePressMs)
    }

    @Test
    fun `All Apps acilinca basis zamani sifirlanir`() {
        val decision = homePressDecision(lastHomePressMs = 10_000L, nowMs = 10_300L)

        assertTrue(decision is HomePressDecision.OpenAllApps)
        assertEquals(0L, decision.nextLastHomePressMs)
    }

    @Test
    fun `cok uzun sureden sonra gelen basis da tek basis sayilir`() {
        // lastHomePressMs=0 (uygulama hiç Home'a basılmamış başlangıç durumu) ve nowMs çok
        // büyük bir zaman damgası olsa da fark 500ms'i aşıyorsa RecordPress olmalı.
        val decision = homePressDecision(lastHomePressMs = 0L, nowMs = 999_999_999L)

        assertTrue(decision is HomePressDecision.RecordPress)
    }
}
