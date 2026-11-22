package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
  @Query("SELECT * FROM hidden_apps ORDER BY addedAtEpochMs DESC")
  fun observeHiddenApps(): Flow<List<HiddenAppEntity>>

  @Query("SELECT * FROM hidden_apps WHERE packageNameHash = :packageNameHash LIMIT 1")
  suspend fun getHiddenAppByHash(packageNameHash: String): HiddenAppEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHiddenApp(entity: HiddenAppEntity)

  @Query("DELETE FROM hidden_apps WHERE packageNameHash = :packageNameHash")
  suspend fun deleteHiddenAppByHash(packageNameHash: String)

  @Query("DELETE FROM hidden_apps")
  suspend fun wipeAllHiddenApps()

  @Query("SELECT * FROM calc_history ORDER BY timestampEpochMs DESC LIMIT 50")
  fun observeCalcHistory(): Flow<List<CalcHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCalcHistory(entry: CalcHistoryEntity)

  @Query("DELETE FROM calc_history")
  suspend fun clearCalcHistory()
}
