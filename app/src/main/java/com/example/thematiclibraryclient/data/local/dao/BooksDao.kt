package com.example.thematiclibraryclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thematiclibraryclient.data.local.entity.BookContentEntity
import com.example.thematiclibraryclient.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BooksDao {

    @Query("SELECT * FROM books WHERE isDeleted = 0")
    fun getBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId AND isDeleted = 0")
    fun getBookById(bookId: Int): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("UPDATE books SET isDeleted = 1, isSynced = 0 WHERE id = :bookId")
    suspend fun markAsDeleted(bookId: Int)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookPhysically(bookId: Int)

    @Query("UPDATE books SET lastPosition = :position, isSynced = 0 WHERE id = :bookId")
    suspend fun updateProgress(bookId: Int, position: Int)

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookEntityById(bookId: Int): BookEntity?

    @Query("SELECT * FROM books WHERE serverId = :serverId LIMIT 1")
    suspend fun getBookByServerId(serverId: Int): BookEntity?

    @Query("SELECT * FROM books WHERE isSynced = 0")
    suspend fun getUnsyncedBooks(): List<BookEntity>

    @Query("SELECT content FROM book_contents WHERE bookId = :bookId")
    suspend fun getBookContent(bookId: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookContent(contentEntity: BookContentEntity)

    @Query("DELETE FROM book_contents WHERE bookId = :bookId")
    suspend fun deleteBookContent(bookId: Int)
}