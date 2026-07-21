package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SmartAccessRankerTest {
    private fun app(
        pkg: String,
        lastUsed: Long = 1L,
        hidden: Boolean = false,
        installed: Boolean = true,
    ) = AppInfo(
        packageName = pkg,
        appName = pkg,
        lastUsedTimestamp = lastUsed,
        isHidden = hidden,
        isInstalled = installed,
    )

    @Test fun `ayni saat dilimi agirligi en yuksektir`() {
        val slot = SmartAccessCandidate(app("slot"), 1f, 0f, 0f, 0f)
        val other = SmartAccessCandidate(app("other"), 0f, 1f, .5f, .5f)
        assertEquals("slot", SmartAccessRanker.rankNow(listOf(other, slot), "launcher").first().packageName)
    }

    @Test fun `gizli kaldirilmis ve launcher uygulamasi dislanir`() {
        val candidates = listOf(
            SmartAccessCandidate(app("hidden", hidden = true), 1f, 1f, 1f, 1f),
            SmartAccessCandidate(app("gone", installed = false), 1f, 1f, 1f, 1f),
            SmartAccessCandidate(app("launcher"), 1f, 1f, 1f, 1f),
            SmartAccessCandidate(app("ok"), 0f, 0f, 0f, 0f),
        )
        assertEquals(listOf("ok"), SmartAccessRanker.rankNow(candidates, "launcher").map { it.packageName })
    }

    @Test fun `paket tekrari uretilmez ve bes slot asılmaz`() {
        val candidates = (1..8).flatMap { index ->
            val candidate = SmartAccessCandidate(app("p$index"), 1f, 1f, 1f, 1f)
            listOf(candidate, candidate)
        }
        val result = SmartAccessRanker.rankNow(candidates, "launcher")
        assertEquals(5, result.size)
        assertEquals(result.size, result.distinctBy { it.packageName }.size)
    }

    @Test fun `recent gercek timestamp ile siralanir`() {
        val result = SmartAccessRanker.recent(
            apps = listOf(app("old", 10), app("new", 30), app("middle", 20)),
            ownPackageName = "launcher",
        )
        assertEquals(listOf("new", "middle", "old"), result.map { it.packageName })
    }

    @Test fun `gecersiz skorlar sifir bir araligina kirpilir`() {
        val candidate = SmartAccessCandidate(app("a"), 2f, -1f, 2f, -1f)
        val score = SmartAccessRanker.score(candidate)
        assertFalse(score < 0f || score > 1f)
    }
}
