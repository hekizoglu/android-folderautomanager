package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo

object SmartAccessDedupePolicy {
    fun visibleUnique(
        apps: Iterable<AppInfo>,
        ownPackageName: String,
        excludedPackages: Set<String> = emptySet(),
    ): List<AppInfo> = apps
        .asSequence()
        .filter { it.isInstalled && !it.isHidden }
        .filter { it.packageName != ownPackageName && it.packageName !in excludedPackages }
        .distinctBy { it.packageName }
        .toList()
}
