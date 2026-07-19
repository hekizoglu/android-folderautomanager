package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Dongu G6 — Yildiz Ekonomisi: seviye esikleri, nextLevelAt/starsToNextLevel/progressToNext
 * sinir degerleri. Plan G6 esikleri: 0-9 Caylak, 10-24 Duzenli, 25-49 Odakli,
 * 50-99 Denge Ustasi, 100+ Usta.
 */
class StarLevelSystemTest {

    @Test
    fun `levelFor boundary values map to expected level`() {
        assertEquals(StarLevelSystem.Level.BEGINNER, StarLevelSystem.levelFor(0))
        assertEquals(StarLevelSystem.Level.BEGINNER, StarLevelSystem.levelFor(9))
        assertEquals(StarLevelSystem.Level.STEADY, StarLevelSystem.levelFor(10))
        assertEquals(StarLevelSystem.Level.STEADY, StarLevelSystem.levelFor(24))
        assertEquals(StarLevelSystem.Level.FOCUSED, StarLevelSystem.levelFor(25))
        assertEquals(StarLevelSystem.Level.FOCUSED, StarLevelSystem.levelFor(49))
        assertEquals(StarLevelSystem.Level.BALANCE_MASTER, StarLevelSystem.levelFor(50))
        assertEquals(StarLevelSystem.Level.BALANCE_MASTER, StarLevelSystem.levelFor(99))
        assertEquals(StarLevelSystem.Level.MASTER, StarLevelSystem.levelFor(100))
        assertEquals(StarLevelSystem.Level.MASTER, StarLevelSystem.levelFor(1000))
    }

    @Test
    fun `levelFor negative stars treated as beginner`() {
        assertEquals(StarLevelSystem.Level.BEGINNER, StarLevelSystem.levelFor(-5))
    }

    @Test
    fun `nextLevelAt returns next threshold except for master`() {
        assertEquals(10, StarLevelSystem.nextLevelAt(0))
        assertEquals(25, StarLevelSystem.nextLevelAt(10))
        assertEquals(50, StarLevelSystem.nextLevelAt(25))
        assertEquals(100, StarLevelSystem.nextLevelAt(50))
        assertNull(StarLevelSystem.nextLevelAt(100))
        assertNull(StarLevelSystem.nextLevelAt(500))
    }

    @Test
    fun `starsToNextLevel counts remaining stars`() {
        assertEquals(10, StarLevelSystem.starsToNextLevel(0))
        assertEquals(1, StarLevelSystem.starsToNextLevel(9))
        // At exactly 10 stars the level becomes STEADY, next threshold is FOCUSED at 25.
        assertEquals(15, StarLevelSystem.starsToNextLevel(10))
        assertNull(StarLevelSystem.starsToNextLevel(100))
    }

    @Test
    fun `progressToNext is zero at level start and near one just before next`() {
        assertEquals(0f, StarLevelSystem.progressToNext(0), 0.0001f)
        assertEquals(0f, StarLevelSystem.progressToNext(10), 0.0001f)
        // 10..24 span is 15; at 24 stars progress = 14/15
        assertEquals(14f / 15f, StarLevelSystem.progressToNext(24), 0.0001f)
        assertEquals(1f, StarLevelSystem.progressToNext(100), 0.0001f)
        assertEquals(1f, StarLevelSystem.progressToNext(9999), 0.0001f)
    }

    @Test
    fun `english labels are distinct and non blank`() {
        val labels = StarLevelSystem.Level.entries.map { it.labelEn }
        assertEquals(labels.size, labels.toSet().size)
        labels.forEach { assertEquals(false, it.isBlank()) }
    }
}
