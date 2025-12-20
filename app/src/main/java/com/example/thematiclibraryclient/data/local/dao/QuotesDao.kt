package com.example.thematiclibraryclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thematiclibraryclient.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotesDao {

    @Query("SELECT * FROM quotes ORDER BY id DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE bookId = :bookId ORDER BY positionStart ASC")
    fun getQuotesForBook(bookId: Int): Flow<List<QuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity)

    @Query("UPDATE quotes SET noteContent = :content WHERE id = :quoteId")
    suspend fun updateNoteContent(quoteId: Int, content: String)

    @Query("DELETE FROM quotes WHERE id = :quoteId")
    suspend fun deleteQuote(quoteId: Int)

    @Query("DELETE FROM quotes")
    suspend fun clearQuotes()
}