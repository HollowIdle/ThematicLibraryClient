package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IQuotesApi
import com.example.thematiclibraryclient.data.remote.model.grouped.toDomainModel
import com.example.thematiclibraryclient.data.remote.model.quotes.CreateQuoteRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.quotes.toDomainModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import jakarta.inject.Inject

class QuotesRemoteRepositoryImpl @Inject constructor(
    private val quotesApi: IQuotesApi
) : IQuotesRemoteRepository {

    override suspend fun getGroupedQuotes(): TResult<List<ShelfGroupDomainModel>, ConnectionExceptionDomainModel> {
        return try {
            val groupedQuotes = quotesApi.getGroupedQuotes()
            TResult.Success(groupedQuotes.map { it.toDomainModel() })
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun getFlatQuotes(): TResult<List<QuoteDomainModel>, ConnectionExceptionDomainModel> {
        return try {
            val flatQuotes = quotesApi.getFlatQuotes()
            TResult.Success(flatQuotes.map { it.toDomainModel() })
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun createQuote(
        bookId: Int,
        text: String,
        start: Int?,
        end: Int?,
        note: String?
    ): TResult<QuoteDomainModel, ConnectionExceptionDomainModel> {
        return try {
            val request = CreateQuoteRequestApiModel(
                selectedText = text,
                positionStart = start,
                positionEnd = end,
                note = note
            )
            val createdQuoteApiModel = quotesApi.createQuote(bookId, request)
            TResult.Success(createdQuoteApiModel.toDomainModel(bookId))
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun deleteQuote(quoteId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            quotesApi.deleteQuote(quoteId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun getQuotesForBook(bookId: Int): TResult<List<QuoteDomainModel>, ConnectionExceptionDomainModel> {
        return try {
            val quotesForBook = quotesApi.getQuotesForBook(bookId)
            TResult.Success(quotesForBook.map { it.toDomainModel(bookId) })
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

}