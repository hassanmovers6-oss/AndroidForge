package com.educationwithhassan.financetools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCalculationDao {
    @Query("SELECT * FROM saved_calculations ORDER BY createdAtEpochMs DESC")
    fun observeHistory(): Flow<List<SavedCalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(entity: SavedCalculationEntity)

    @Query("DELETE FROM saved_calculations WHERE id = :id")
    suspend fun deleteCalculation(id: Long)

    @Query("DELETE FROM saved_calculations")
    suspend fun clearAllHistory()

    @Query("SELECT toolId FROM favorite_tools ORDER BY addedAtEpochMs DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteToolEntity)

    @Query("DELETE FROM favorite_tools WHERE toolId = :toolId")
    suspend fun removeFavorite(toolId: String)

    @Query("SELECT toolId FROM recent_tools ORDER BY lastOpenedEpochMs DESC LIMIT 12")
    fun observeRecentIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecent(recent: RecentToolEntity)
}
