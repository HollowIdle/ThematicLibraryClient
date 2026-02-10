package com.example.thematiclibraryclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thematiclibraryclient.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarksDao {

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND isDeleted = 0 ORDER BY position ASC")
    fun getBookmarksForBook(bookId: Int): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    suspend fun getBookmarkEntityById(bookmarkId: Int): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE isDeleted = 1")
    suspend fun getDeletedBookmarks(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("UPDATE bookmarks SET isDeleted = 1, isSynced = 0 WHERE id = :bookmarkId")
    suspend fun markAsDeleted(bookmarkId: Int)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkPhysically(bookmarkId: Int)

    @Query("SELECT * FROM bookmarks WHERE serverId = :serverId LIMIT 1")
    suspend fun getBookmarkByServerId(serverId: Int): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE isSynced = 0")
    suspend fun getUnsyncedBookmarks(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}