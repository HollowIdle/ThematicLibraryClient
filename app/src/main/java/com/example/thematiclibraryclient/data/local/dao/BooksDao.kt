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

    @Query("SELECT * FROM books")
    fun getBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Int): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: Int)

    @Query("UPDATE books SET lastPosition = :position WHERE id = :bookId")
    suspend fun updateProgress(bookId: Int, position: Int)

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookEntityById(bookId: Int): BookEntity?

    // Book content

    @Query("SELECT content FROM book_contents WHERE bookId = :bookId")
    suspend fun getBookContent(bookId: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookContent(contentEntity: BookContentEntity)

    @Query("DELETE FROM book_contents WHERE bookId = :bookId")
    suspend fun deleteBookContent(bookId: Int)
}