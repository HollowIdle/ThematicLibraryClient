package com.example.thematiclibraryclient.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thematiclibraryclient.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotesDao {

    @Query("SELECT * FROM quotes WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :quoteId")
    suspend fun getQuoteEntityById(quoteId: Int): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE bookId = :bookId AND isDeleted = 0 ORDER BY positionStart ASC")
    fun getQuotesForBook(bookId: Int): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isDeleted = 1")
    suspend fun getDeletedQuotes(): List<QuoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Query("UPDATE quotes SET noteContent = :content, isSynced = 0 WHERE id = :quoteId")
    suspend fun updateNoteContent(quoteId: Int, content: String)

    @Query("UPDATE quotes SET isDeleted = 1, isSynced = 0 WHERE id = :quoteId")
    suspend fun markAsDeleted(quoteId: Int)

    @Query("DELETE FROM quotes WHERE id = :quoteId")
    suspend fun deleteQuotePhysically(quoteId: Int)

    @Query("DELETE FROM quotes")
    suspend fun clearQuotes()

    @Query("SELECT * FROM quotes WHERE serverId = :serverId LIMIT 1")
    suspend fun getQuoteByServerId(serverId: Int): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE isSynced = 0")
    suspend fun getUnsyncedQuotes(): List<QuoteEntity>

    @Query("DELETE FROM quotes")
    suspend fun deleteAllQuotes()
}