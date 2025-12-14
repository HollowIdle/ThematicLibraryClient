package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel

interface IQuotesRemoteRepository {
    suspend fun getGroupedQuotes(): TResult<List<ShelfGroupDomainModel>, ConnectionExceptionDomainModel>
    suspend fun getFlatQuotes(): TResult<List<QuoteDomainModel>, ConnectionExceptionDomainModel>
    suspend fun createQuote(bookId: Int, text: String, start: Int?, end: Int?, note: String?): TResult<QuoteDomainModel,
            ConnectionExceptionDomainModel>
    suspend fun deleteQuote(quoteId: Int): TResult<Unit, ConnectionExceptionDomainModel>
    suspend fun getQuotesForBook(bookId: Int): TResult<List<QuoteDomainModel>, ConnectionExceptionDomainModel>
}