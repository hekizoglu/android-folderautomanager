package com.armutlu.apporganizer.domain.usecase.privacy

import com.armutlu.apporganizer.domain.usecase.privacy.PrivacyAnalyzer.AppPermissionSnapshot
import com.armutlu.apporganizer.domain.usecase.privacy.PrivacyAnalyzer.PermissionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PrivacyAnalyzer.buildReport() saf mantik testleri — sahte izin snapshot'lariyla
 * gruplama/sayim dogrulamasi. PackageManager'a bagimlilik yok.
 */
class PrivacyAnalyzerTest {

    private val cameraGroup = PrivacyAnalyzer.SENSITIVE_GROUPS.first { it.id == "camera" }
    private val locationGroup = PrivacyAnalyzer.SENSITIVE_GROUPS.first { it.id == "location" }

    private fun snapshot(
        pkg: String,
        name: String,
        requested: Set<String>,
        granted: Set<String>,
    ) = AppPermissionSnapshot(
        packageName = pkg,
        appName = name,
        requestedPermissions = requested,
        grantedPermissions = granted,
    )

    // ── bos girdi ────────────────────────────────────────────────────────────

    @Test
    fun `empty snapshots produce empty groups`() {
        val report = PrivacyAnalyzer.buildReport(emptyList())

        assertEquals(PrivacyAnalyzer.SENSITIVE_GROUPS.size, report.groups.size)
        assertTrue(report.groups.all { it.grantedCount == 0 && it.requestedNotGrantedCount == 0 })
        assertTrue(report.isAllClear)
    }

    // ── verilmis izin sayimi ────────────────────────────────────────────────

    @Test
    fun `app with granted camera permission counted in camera group`() {
        val snapshots = listOf(
            snapshot(
                "com.example.cam",
                "CamApp",
                requested = setOf("android.permission.CAMERA"),
                granted = setOf("android.permission.CAMERA"),
            ),
        )

        val report = PrivacyAnalyzer.buildReport(snapshots)
        val cameraReport = report.groups.first { it.group.id == "camera" }

        assertEquals(1, cameraReport.grantedCount)
        assertEquals("com.example.cam", cameraReport.grantedApps.first().packageName)
        assertEquals(0, cameraReport.requestedNotGrantedCount)
        assertFalse(report.isAllClear)
    }

    // ── istedi ama verilmedi ────────────────────────────────────────────────

    @Test
    fun `app that requested but was not granted counted separately`() {
        val snapshots = listOf(
            snapshot(
                "com.example.denied",
                "DeniedApp",
                requested = setOf("android.permission.CAMERA"),
                granted = emptySet(),
            ),
        )

        val report = PrivacyAnalyzer.buildReport(snapshots)
        val cameraReport = report.groups.first { it.group.id == "camera" }

        assertEquals(0, cameraReport.grantedCount)
        assertEquals(1, cameraReport.requestedNotGrantedCount)
    }

    // ── grup icinde birden fazla izin (konum) ──────────────────────────────

    @Test
    fun `location group matches any of its manifest permissions`() {
        val snapshots = listOf(
            snapshot(
                "com.example.fine",
                "FineLocApp",
                requested = setOf("android.permission.ACCESS_FINE_LOCATION"),
                granted = setOf("android.permission.ACCESS_FINE_LOCATION"),
            ),
            snapshot(
                "com.example.bg",
                "BgLocApp",
                requested = setOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_BACKGROUND_LOCATION",
                ),
                granted = setOf("android.permission.ACCESS_COARSE_LOCATION"),
            ),
        )

        val report = PrivacyAnalyzer.buildReport(snapshots)
        val locationReport = report.groups.first { it.group.id == "location" }

        assertEquals(2, locationReport.grantedCount)
    }

    // ── ilgisiz izin gruba dahil edilmemeli ────────────────────────────────

    @Test
    fun `app without requesting the group permission is excluded entirely`() {
        val snapshots = listOf(
            snapshot(
                "com.example.other",
                "OtherApp",
                requested = setOf("android.permission.INTERNET"),
                granted = setOf("android.permission.INTERNET"),
            ),
        )

        val report = PrivacyAnalyzer.buildReport(snapshots)

        report.groups.forEach { groupReport ->
            assertEquals(0, groupReport.grantedCount)
            assertEquals(0, groupReport.requestedNotGrantedCount)
        }
        assertTrue(report.isAllClear)
    }

    // ── siralama ─────────────────────────────────────────────────────────────

    @Test
    fun `granted apps sorted alphabetically by name`() {
        val snapshots = listOf(
            snapshot("com.z", "Zebra", setOf("android.permission.CAMERA"), setOf("android.permission.CAMERA")),
            snapshot("com.a", "Apple", setOf("android.permission.CAMERA"), setOf("android.permission.CAMERA")),
        )

        val report = PrivacyAnalyzer.buildReport(snapshots)
        val cameraReport = report.groups.first { it.group.id == "camera" }

        assertEquals(listOf("Apple", "Zebra"), cameraReport.grantedApps.map { it.appName })
    }

    // ── custom grup listesi ile calisir ────────────────────────────────────

    @Test
    fun `buildReport works with custom group subset`() {
        val customGroups = listOf(cameraGroup, locationGroup)
        val snapshots = listOf(
            snapshot("com.cam", "CamApp", setOf("android.permission.CAMERA"), setOf("android.permission.CAMERA")),
        )

        val report = PrivacyAnalyzer.buildReport(snapshots, groups = customGroups)

        assertEquals(2, report.groups.size)
        assertEquals(1, report.groups.first { it.group.id == "camera" }.grantedCount)
    }

    @Test
    fun `sensitive group ids are unique`() {
        // Sanity: SENSITIVE_GROUPS grup id'leri benzersiz olmali (kullanim noktalari icin garanti).
        val ids = PrivacyAnalyzer.SENSITIVE_GROUPS.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }
}
