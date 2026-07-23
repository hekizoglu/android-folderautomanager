package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.ScoreType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döngü P1.5 — Arama sonuçları skor hesaplamasının birim testleri.
 * Pure mantık testleri — database/Compose bağımlılığı yok.
 */
class SearchScoringTest {

    @Test
    fun `tam isim eslemesi 100 puan EXACT`() {
        val score = SearchRepository.calculateScore("Telegram", "Telegram", "")
        assertEquals(100, score.score)
        assertEquals(ScoreType.EXACT, score.type)
    }

    @Test
    fun `tam isim lowercase eslemesi 100 puan`() {
        val score = SearchRepository.calculateScore("gmail", "Gmail", "")
        assertEquals(100, score.score)
        assertEquals(ScoreType.EXACT, score.type)
    }

    @Test
    fun `kelime baslangici PREFIX 90+ puan`() {
        val score = SearchRepository.calculateScore("Chat", "ChatGPT", "")
        assertEquals(95, score.score)
        assertEquals(ScoreType.PREFIX, score.type)
    }

    @Test
    fun `contains 75-89 puan arasinda`() {
        val score = SearchRepository.calculateScore("mail", "Gmail", "email@example.com")
        assertTrue("Score ${score.score} should be between 75-89", score.score in 75..89)
        assertEquals(ScoreType.CONTAINS, score.type)
    }

    @Test
    fun `fuzzy benzerlik 50-74 puan arasinda`() {
        val score = SearchRepository.calculateScore("Telegam", "Telegram", "")
        assertTrue("Score ${score.score} should be between 50-74", score.score in 50..74)
        assertEquals(ScoreType.FUZZY, score.type)
    }

    @Test
    fun `buyuk kucuk harf farketmiyor Turkce locale`() {
        val score1 = SearchRepository.calculateScore("TELEGRAM", "telegram", "")
        val score2 = SearchRepository.calculateScore("telegram", "TELEGRAM", "")
        assertEquals(100, score1.score)
        assertEquals(100, score2.score)
    }

    @Test
    fun `elesme yoksa skor 0 ve NONE tipi`() {
        val score = SearchRepository.calculateScore("xyz123", "Telegram", "messaging app")
        assertEquals(0, score.score)
        assertEquals(ScoreType.NONE, score.type)
    }

    @Test
    fun `subtitle da arama yapilir`() {
        val score = SearchRepository.calculateScore("messaging", "Telegram", "messaging app")
        assertTrue(score.score > 0)  // subtitle'da bulunur
    }
}
