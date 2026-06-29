package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.armutlu.apporganizer.domain.models.SearchDocument

/**
 * FTS5 arama DAO'su.
 *
 * Sorgu mantığı:
 * - Prefix match: `"wa"*` → "WhatsApp", "Wallpaper" vb. ile eşleşir
 * - BOOLEAN mod: çoklu terim desteği (örn: "what app" → her iki terimi içerenler)
 * - bm25() skoru: metin eşleşme kalitesine göre sıralar
 * - source_group sıralaması: app'ler kategori sonuçlarından önce gelir
 */
@Dao
interface SearchDao {

    /**
     * Ana arama sorgusu.
     *
     * @param query FTS5 sorgu metni — çağıran taraf prefix wildcard (*) eklemelidir
     * @param limit Maksimum sonuç sayısı (varsayılan 50)
     * @return source_group, bm25 sırasında gruplandırılmış SearchDocument'ler
     */
    @Query("""
        SELECT * FROM search_documents
        WHERE title LIKE '%' || :query || '%' OR subtitle LIKE '%' || :query || '%'
        ORDER BY source_group ASC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<SearchDocument>

    /**
     * Toplu indeks ekleme. İlk bootstrap ve tam reindex için.
     * REPLACE stratejisi: aynı docId varsa günceller (FTS trigger'ları da çalışır).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<SearchDocument>): List<Long>

    /**
     * Tekil döküman ekleme/güncelleme. Delta update için.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: SearchDocument): Long

    /**
     * Kaynak tipi + kaynak ID'sine göre silme.
     * Uygulama kaldırıldığında veya kategori silindiğinde kullanılır.
     */
    @Query("DELETE FROM search_documents WHERE source_type = :type AND source_id = :id")
    suspend fun delete(type: String, id: String): Int

    /**
     * Kategori adı değiştiğinde, o kategoriye ait tüm app dökümanlarının
     * subtitle alanını günceller ve FTS indeksini tazeler.
     */
    @Query("""
        UPDATE search_documents
        SET subtitle = :newCategoryName, last_modified = :ts
        WHERE source_type = 'app' AND subtitle = :oldCategoryName
    """)
    suspend fun updateCategoryRefs(oldCategoryName: String, newCategoryName: String, ts: Long): Int

    /**
     * Tüm indeksi temizle. Sadece tam reindex öncesi kullanılır.
     */
    @Query("DELETE FROM search_documents")
    suspend fun deleteAll()

    /**
     * Debug/test için indekslenmiş döküman sayısı.
     */
    @Query("SELECT COUNT(*) FROM search_documents")
    suspend fun count(): Int

    /**
     * Belirli bir kaynaktaki tüm dökümanları sil.
     * Örn: Contacts kapatıldığında tüm contact dökümanlarını temizler.
     */
    @Query("DELETE FROM search_documents WHERE source_type = :type")
    suspend fun deleteBySource(type: String): Int
}
