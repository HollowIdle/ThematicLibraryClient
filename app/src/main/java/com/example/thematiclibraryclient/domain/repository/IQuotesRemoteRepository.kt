package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import kotlinx.coroutines.flow.Flow

interface IQuotesRemoteRepository {

    fun getFlatQuotes(): Flow<List<QuoteDomainModel>>

    fun getGroupedQuotes(): Flow<List<ShelfGroupDomainModel>>

    fun getQuotesForBook(bookId: Int): Flow<List<QuoteDomainModel>>

    suspend fun syncPendingChanges(): TResult<Unit, Exception>

    suspend fun refreshQuotes(): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun refreshGroupedQuotes(): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun createQuote(bookId: Int, text: String, start: Int, end: Int, note: String?, locatorData: String?): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun deleteQuote(quoteId: Int): TResult<Unit, ConnectionExceptionDomainModel>
}