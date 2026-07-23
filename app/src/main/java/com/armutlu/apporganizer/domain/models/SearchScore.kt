package com.armutlu.apporganizer.domain.models

/**
 * Döngü P1.5 — Arama sonuçlarının korunabilirlik puanı.
 *
 * Her SearchDocument tam eşleşme derecesi (EXACT, PREFIX, CONTAINS, FUZZY, PHONETIC, NONE)
 * değerlendirilir — UI sıralama + "Google'da ara" fallback karar için kullanılır.
 */
data class SearchScore(
    val type: ScoreType,
    val score: Int,      // 0-100, yüksek = daha iyi match
    val detail: String   // Kullanıcı açıklaması: "Tam isim eşleşmesi", vb.
)

/**
 * Skor türü ve standart puan aralığı.
 *
 * - EXACT:    tam kelimenin eşleşmesi (büyük-küçük harf yok sayılır, Türkçe I/İ uyumlu)
 * - PREFIX:   aranılan kelimenin başında bulunması (örn "Chat" → "ChatGPT")
 * - CONTAINS: yazılı metinde herhangi yerde bulunması
 * - FUZZY:    Levenshtein mesafesiyle benzerlik (yazım hatası toleransı)
 * - PHONETIC: Türkçe fonetik benzerlik (İ↔I, Ş↔S, vb.) — ileride impl, şimdi CONTAINS'e düşer
 * - NONE:     eşleşme yok (normalde sonuç listesine girmez)
 */
enum class ScoreType {
    EXACT,      // 100
    PREFIX,     // 90-99
    CONTAINS,   // 75-89
    FUZZY,      // 50-74
    PHONETIC,   // 40-49 (ileride Levenshtein fine-tuning)
    NONE        // 0
}
