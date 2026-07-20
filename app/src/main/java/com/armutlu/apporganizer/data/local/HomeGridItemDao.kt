package com.armutlu.apporganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Faz S (Serbest Sürükle-Bırak Ana Ekran Sistemi) — S1 veri modeli DAO'su. Şu an hiçbir
 * ViewModel/UI bu DAO'yu çağırmıyor; gelecekteki serbest 2D yerleşim için altyapı.
 */
@Dao
interface HomeGridItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HomeGridItemEntity>)

    @Query("SELECT * FROM home_grid_items WHERE screenIndex = :screenIndex")
    fun observeByScreen(screenIndex: Int): Flow<List<HomeGridItemEntity>>

    @Query("DELETE FROM home_grid_items WHERE itemId = :itemId")
    suspend fun deleteById(itemId: String)

    @Query("DELETE FROM home_grid_items")
    suspend fun deleteAll()
}
