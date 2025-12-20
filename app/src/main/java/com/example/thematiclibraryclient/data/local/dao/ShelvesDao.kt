package com.example.thematiclibraryclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thematiclibraryclient.data.local.entity.ShelfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelvesDao {

    @Query("SELECT * FROM shelves WHERE isDeleted = 0 ORDER BY name ASC")
    fun getShelves(): Flow<List<ShelfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelves(shelves: List<ShelfEntity>)

    @Query("UPDATE shelves SET isDeleted = 1, isSynced = 0 WHERE id = :shelfId")
    suspend fun markAsDeleted(shelfId: Int)

    @Query("DELETE FROM shelves WHERE id = :shelfId")
    suspend fun deleteShelfPhysically(shelfId: Int)

    @Query("DELETE FROM shelves")
    suspend fun clearShelves()

    @Query("SELECT * FROM shelves WHERE serverId = :serverId LIMIT 1")
    suspend fun getShelfByServerId(serverId: Int): ShelfEntity?

    @Query("SELECT * FROM shelves WHERE isSynced = 0")
    suspend fun getUnsyncedShelves(): List<ShelfEntity>
}