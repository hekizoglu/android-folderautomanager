package com.armutlu.apporganizer.data.local

/**
 * FTS5 sonuç satırı için mapping data class.
 * search_fts sanal tablosu Migration 8→9'daki raw SQL ile yönetilir;
 * Room @Fts5/@Entity kullanılmaz — kapt uyumsuzluğunu önler.
 */
data class SearchFts(
    val searchText: String,
    val keywords: String = ""
)
