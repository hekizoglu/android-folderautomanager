package com.armutlu.apporganizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * fuzzyEditDistance fonksiyonu için unit testler.
 * AllAppsDrawer.kt'deki private fonksiyonu kopyalayıp test ediyoruz.
 */
class FuzzySearchTest {

    private fun editDistance(a: String, b: String): Int {
        val s = a.take(20); val t = b.take(20)
        if (s == t) return 0
        if (s.isEmpty()) return t.length
        if (t.isEmpty()) return s.length
        val dp = Array(s.length + 1) { IntArray(t.length + 1) { 0 } }
        for (i in 0..s.length) dp[i][0] = i
        for (j in 0..t.length) dp[0][j] = j
        for (i in 1..s.length) for (j in 1..t.length) {
            dp[i][j] = if (s[i - 1] == t[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[s.length][t.length]
    }

    private fun threshold(q: String) = maxOf(1, q.length / 4)

    @Test
    fun `telegrab should match telegram`() {
        val dist = editDistance("telegram", "telegrab")
        assertTrue("telegrab->telegram mesafe $dist, beklenen <= 1", dist <= 1)
    }

    @Test
    fun `exact match returns 0`() {
        assertEquals(0, editDistance("whatsapp", "whatsapp"))
    }

    @Test
    fun `one char typo within threshold`() {
        val q = "insagram"
        val dist = editDistance("instagram", q)
        assertTrue("insagram->instagram mesafe $dist <= ${threshold(q)}", dist <= threshold(q))
    }

    @Test
    fun `two char typo on long query within threshold`() {
        val q = "youtubb"
        val dist = editDistance("youtube", q)
        assertTrue("youtubb->youtube mesafe $dist <= ${threshold(q)}", dist <= threshold(q))
    }

    @Test
    fun `completely different words should not match`() {
        val q = "xyz"
        val dist = editDistance("whatsapp", q)
        assertTrue("xyz vs whatsapp mesafe $dist > ${threshold(q)}", dist > threshold(q))
    }

    @Test
    fun `empty query returns target length`() {
        assertEquals(8, editDistance("whatsapp", ""))
    }

    @Test
    fun `short typo twiter matches twitter`() {
        val dist = editDistance("twitter", "twiter")
        assertTrue("twiter->twitter dist=$dist <= 1", dist <= 1)
    }
}
