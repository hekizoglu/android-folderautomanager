package com.armutlu.apporganizer.presentation.viewmodel

import com.armutlu.apporganizer.utils.NotificationAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NotificationReportUiState.from() saf durum eşlemesi testleri (Döngü 224).
 *
 * UX sözleşmesi:
 * - Veri VARSA her zaman rapor gösterilir; izin/analiz sorunları banner bayrağı olur.
 * - Veri YOKSA sebep önceliği: izin eksik > analiz kapalı > veri toplanıyor.
 */
class NotificationReportUiStateTest {

    private fun emptyReport() = NotificationAnalyzer.Report(
        totalNotifications = 0,
        appStats = emptyList(),
        mostTalkative = emptyList(),
        disturbing = emptyList(),
        distracting = emptyList(),
    )

    private fun reportWithData(total: Int = 42) = NotificationAnalyzer.Report(
        totalNotifications = total,
        appStats = emptyList(),
        mostTalkative = emptyList(),
        disturbing = emptyList(),
        distracting = emptyList(),
    )

    // ── yükleme ──────────────────────────────────────────────────────────────

    @Test
    fun `null report maps to Loading`() {
        val state = NotificationReportUiState.from(null, permissionGranted = true, analyticsEnabled = true)
        assertEquals(NotificationReportUiState.Loading, state)
    }

    // ── boş rapor: sebep ayrımı ──────────────────────────────────────────────

    @Test
    fun `empty report without permission maps to PermissionMissing`() {
        val state = NotificationReportUiState.from(emptyReport(), permissionGranted = false, analyticsEnabled = true)
        assertEquals(NotificationReportUiState.PermissionMissing, state)
    }

    @Test
    fun `permission missing wins over analytics disabled when both apply`() {
        val state = NotificationReportUiState.from(emptyReport(), permissionGranted = false, analyticsEnabled = false)
        assertEquals(NotificationReportUiState.PermissionMissing, state)
    }

    @Test
    fun `empty report with analytics off maps to AnalyticsDisabled`() {
        val state = NotificationReportUiState.from(emptyReport(), permissionGranted = true, analyticsEnabled = false)
        assertEquals(NotificationReportUiState.AnalyticsDisabled, state)
    }

    @Test
    fun `empty report with everything enabled maps to CollectingData`() {
        val state = NotificationReportUiState.from(emptyReport(), permissionGranted = true, analyticsEnabled = true)
        assertEquals(NotificationReportUiState.CollectingData, state)
    }

    // ── dolu rapor: her zaman Ready, sorunlar bayrak olur ────────────────────

    @Test
    fun `report with data maps to Ready without warning flags`() {
        val state = NotificationReportUiState.from(reportWithData(), permissionGranted = true, analyticsEnabled = true)
        assertTrue(state is NotificationReportUiState.Ready)
        state as NotificationReportUiState.Ready
        assertEquals(42, state.report.totalNotifications)
        assertFalse(state.permissionMissing)
        assertFalse(state.analyticsDisabled)
    }

    @Test
    fun `report with data but no permission stays Ready with permission flag`() {
        val state = NotificationReportUiState.from(reportWithData(), permissionGranted = false, analyticsEnabled = true)
        assertTrue(state is NotificationReportUiState.Ready)
        state as NotificationReportUiState.Ready
        assertTrue(state.permissionMissing)
        assertFalse(state.analyticsDisabled)
    }

    @Test
    fun `report with data but analytics off stays Ready with analytics flag`() {
        val state = NotificationReportUiState.from(reportWithData(), permissionGranted = true, analyticsEnabled = false)
        assertTrue(state is NotificationReportUiState.Ready)
        state as NotificationReportUiState.Ready
        assertFalse(state.permissionMissing)
        assertTrue(state.analyticsDisabled)
    }

    @Test
    fun `report with data and both problems shows both flags`() {
        val state = NotificationReportUiState.from(reportWithData(), permissionGranted = false, analyticsEnabled = false)
        assertTrue(state is NotificationReportUiState.Ready)
        state as NotificationReportUiState.Ready
        assertTrue(state.permissionMissing)
        assertTrue(state.analyticsDisabled)
    }
}
