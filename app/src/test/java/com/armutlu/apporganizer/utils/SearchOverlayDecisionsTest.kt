package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.FileIndexState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchOverlayDecisionsTest {

    @Test
    fun shouldShowFilesPermissionHint_onlyWhenQueryActiveAndPermissionRequired() {
        assertTrue(
            SearchOverlayDecisions.shouldShowFilesPermissionHint(
                query = "pdf",
                filesOn = true,
                filesIndexState = FileIndexState.PermissionRequired,
            )
        )
        assertFalse(
            SearchOverlayDecisions.shouldShowFilesPermissionHint(
                query = "",
                filesOn = true,
                filesIndexState = FileIndexState.PermissionRequired,
            )
        )
        assertFalse(
            SearchOverlayDecisions.shouldShowFilesPermissionHint(
                query = "pdf",
                filesOn = true,
                filesIndexState = FileIndexState.Ready(itemCount = 12, lastIndexedAt = 1L),
            )
        )
    }

    @Test
    fun shouldShowWebFallback_requiresNoResultsAndNoPermissionHints() {
        assertTrue(
            SearchOverlayDecisions.shouldShowWebFallback(
                query = "signal",
                webFallbackEnabled = true,
                appCount = 0,
                folderCount = 0,
                contactCount = 0,
                settingCount = 0,
                fileCount = 0,
            )
        )
        assertFalse(
            SearchOverlayDecisions.shouldShowWebFallback(
                query = "signal",
                webFallbackEnabled = true,
                appCount = 1,
                folderCount = 0,
                contactCount = 0,
                settingCount = 0,
                fileCount = 0,
            )
        )
        assertFalse(
            SearchOverlayDecisions.shouldShowWebFallback(
                query = "signal",
                webFallbackEnabled = true,
                appCount = 0,
                folderCount = 0,
                contactCount = 0,
                settingCount = 0,
                fileCount = 0,
                showFilesPermissionHint = true,
            )
        )
    }
}
