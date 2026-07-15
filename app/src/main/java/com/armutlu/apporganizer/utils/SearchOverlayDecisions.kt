package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.FileIndexState

object SearchOverlayDecisions {
    fun shouldShowFilesPermissionHint(
        query: String,
        filesOn: Boolean,
        filesIndexState: FileIndexState,
        deniedInSession: Boolean = false,
    ): Boolean {
        return query.isNotBlank() &&
            filesOn &&
            !deniedInSession &&
            filesIndexState is FileIndexState.PermissionRequired
    }

    fun shouldShowWebFallback(
        query: String,
        webFallbackEnabled: Boolean,
        appCount: Int,
        folderCount: Int,
        contactCount: Int,
        settingCount: Int,
        fileCount: Int,
        showContactsPermissionHint: Boolean = false,
        showFilesPermissionHint: Boolean = false,
    ): Boolean {
        return webFallbackEnabled &&
            query.trim().length >= 2 &&
            appCount == 0 &&
            folderCount == 0 &&
            contactCount == 0 &&
            settingCount == 0 &&
            fileCount == 0 &&
            !showContactsPermissionHint &&
            !showFilesPermissionHint
    }
}
