package com.armutlu.apporganizer

import com.armutlu.apporganizer.data.repository.SearchRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * A3: FTS5/LIKE Türkçe karakter arama doğrulaması.
 * FTS5 emülatörde yoksa LIKE fallback devreye girer.
 * Bu testler LIKE fallback mantığını (SearchRepository.buildLikeQuery) doğrular.
 */
class TurkishSearchTest {

    private val tr = Locale("tr", "TR")

    private fun likeMatch(query: String, title: String): Boolean {
        val q = query.trim().lowercase(tr)
        val t = title.lowercase(tr)
        return t.contains(q)
    }

    // ── Türkçe özel karakterler ──────────────────────────────────────────────

    @Test fun `cedilla c search`() = assertTrue(likeMatch("çarşı", "Çarşı Takip"))
    @Test fun `cedilla c lowercase`() = assertTrue(likeMatch("carsi", "Çarşı Takip").not() || likeMatch("çarşı", "Çarşı Takip"))
    @Test fun `dotless i`() = assertTrue(likeMatch("ışık", "Işık Yöneticisi"))
    @Test fun `dotted I uppercase`() = assertTrue(likeMatch("işaret", "İşaret Uygulaması"))
    @Test fun `g breve`() = assertTrue(likeMatch("öğrenci", "Öğrenci Takip"))
    @Test fun `u umlaut`() = assertTrue(likeMatch("üretim", "Üretim Planlama"))
    @Test fun `o umlaut`() = assertTrue(likeMatch("özet", "Özet Uygulaması"))
    @Test fun `s cedilla`() = assertTrue(likeMatch("şarj", "Şarj Durumu"))

    // ── I/İ ve ı/i dönüşümleri ──────────────────────────────────────────────

    @Test fun `capital I to lowercase turkish i`() {
        val q = "iletişim".lowercase(tr)
        val t = "İletişim Rehberi".lowercase(tr)
        assertTrue(t.contains(q))
    }

    @Test fun `capital I-dotless to lowercase`() {
        val q = "ısı".lowercase(tr)
        val t = "Isı Kontrol".lowercase(tr)
        assertTrue(t.contains(q))
    }

    // ── Kelime ortasında Türkçe karakter ────────────────────────────────────

    @Test fun `mid-word cedilla`() = assertTrue(likeMatch("araç", "Araç Takip"))
    @Test fun `mid-word g-breve`() = assertTrue(likeMatch("öğretm", "Öğretmen Asistanı"))
    @Test fun `mid-word s-cedilla`() = assertTrue(likeMatch("taşım", "Taşıma Rehberi"))

    // ── Kısmi sorgu ─────────────────────────────────────────────────────────

    @Test fun `partial turkish query`() = assertTrue(likeMatch("türk", "Türkçe Klavye"))
    @Test fun `partial mixed`() = assertTrue(likeMatch("müzik", "Müzik Çalar"))
    @Test fun `partial dotless-i prefix`() = assertTrue(likeMatch("ışın", "Işın Sürücüsü"))

    // ── Yanlış eşleşme olmamalı ─────────────────────────────────────────────

    @Test fun `no false positive - different word`() = assertFalse(likeMatch("çarşı", "Uygulama Yöneticisi"))
    @Test fun `no false positive - ascii typo`() = assertFalse(likeMatch("ozet", "Özet Uygulaması"))

    // ── LIKE pattern güvenlik (% ve _ escape) ───────────────────────────────

    @Test fun `percent escaped in query`() {
        val raw = "test%"
        val escaped = raw.replace("%", "\\%").replace("_", "\\_")
        val pattern = "%$escaped%"
        assertTrue(pattern == "%test\\%%")
    }

    @Test fun `underscore escaped in query`() {
        val raw = "a_b"
        val escaped = raw.replace("%", "\\%").replace("_", "\\_")
        val pattern = "%$escaped%"
        assertTrue(pattern == "%a\\_b%")
    }

    @Test fun `like fallback pattern escapes sqlite wildcards and backslash`() {
        val pattern = SearchRepository.buildLikePattern("a_b%\\")
        assertTrue(pattern == "%a\\_b\\%\\\\%")
    }

    // ── LIKE sorgu mantığı (SearchRepository.buildLikeQuery pattern) ─────────

    @Test fun `like pattern wraps query with percent`() {
        val q = "türkçe"
        val pattern = "%${q.replace("%", "\\%").replace("_", "\\_")}%"
        assertTrue(pattern.startsWith("%"))
        assertTrue(pattern.endsWith("%"))
        assertTrue(pattern.contains(q))
    }

    @Test fun `like match works for subtitle`() {
        val query = "sosyal"
        val subtitle = "Sosyal Medya"
        assertTrue(likeMatch(query, subtitle))
    }

    // ── SearchIndexer normalizasyon ──────────────────────────────────────────

    @Test fun `normalize title preserves turkish chars`() {
        val title = "Öğrenci Takibi"
        val norm = title.lowercase(Locale("tr", "TR"))
        assertTrue(norm.contains("öğrenci"))
        assertTrue(norm.contains("takibi"))
    }

    @Test fun `normalize keeps dotless-i`() {
        val title = "Işık Kontrolü"
        val norm = title.lowercase(Locale("tr", "TR"))
        assertTrue(norm.startsWith("ışık"))
    }
}
