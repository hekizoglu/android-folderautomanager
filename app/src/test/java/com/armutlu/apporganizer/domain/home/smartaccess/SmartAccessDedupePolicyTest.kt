package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartAccessDedupePolicyTest {
    @Test fun `ilk gorunur paket korunur ve gecersizler dislanir`() {
        fun app(pkg: String, hidden: Boolean = false, installed: Boolean = true) =
            AppInfo(packageName = pkg, appName = pkg, isHidden = hidden, isInstalled = installed)
        val result = SmartAccessDedupePolicy.visibleUnique(
            apps = listOf(app("a"), app("a"), app("hidden", true), app("gone", installed = false), app("launcher")),
            ownPackageName = "launcher",
        )
        assertEquals(listOf("a"), result.map { it.packageName })
    }
}
