package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.dao.QuotesDao
import com.example.thematiclibraryclient.data.local.dao.ShelvesDao
import com.example.thematiclibraryclient.data.local.entity.QuoteEntity
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IQuotesApi
import com.example.thematiclibraryclient.data.remote.model.quotes.CreateQuoteRequestApiModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.BookGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuotesRemoteRepositoryImpl @Inject constructor(
    private val quotesApi: IQuotesApi,
    private val quotesDao: QuotesDao,
    private val booksDao: BooksDao,
    private val shelvesDao: ShelvesDao
) : IQuotesRemoteRepository {

    override fun getFlatQuotes(): Flow<List<QuoteDomainModel>> {
        return quotesDao.getAllQuotes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getQuotesForBook(bookId: Int): Flow<List<QuoteDomainModel>> {
        return quotesDao.getQuotesForBook(bookId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getGroupedQuotes(): Flow<List<ShelfGroupDomainModel>> {
        return combine(
            shelvesDao.getShelves(),
            booksDao.getBooks(),
            quotesDao.getAllQuotes()
        ) { shelves, books, quotes ->

            shelves.map { shelf ->
                val shelfBooks = books.filter { it.shelfIds.contains(shelf.id) }

                val bookGroups = shelfBooks.mapNotNull { book ->
                    val bookQuotes = quotes.filter { it.bookId == book.id }

                    if (bookQuotes.isNotEmpty()) {
                        BookGroupDomainModel(
                            bookId = book.id,
                            bookTitle = book.title,
                            quotes = bookQuotes.map { quote ->
                                QuoteGroupDomainModel(
                                    id = quote.id,
                                    selectedText = quote.selectedText,
                                    positionStart = quote.positionStart,
                                    positionEnd = quote.positionEnd,
                                    noteContent = quote.noteContent
                                )
                            }
                        )
                    } else {
                        null
                    }
                }

                ShelfGroupDomainModel(
                    shelfId = shelf.id,
                    shelfName = shelf.name,
                    books = bookGroups
                )
            }.filter { it.books.isNotEmpty() }
        }
    }

    override suspend fun refreshQuotes(): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiQuotes = quotesApi.getFlatQuotes()

            val entities = apiQuotes.map { apiQuote ->
                val existing = quotesDao.getQuoteByServerId(apiQuote.id)

                QuoteEntity(
                    id = existing?.id ?: 0,
                    serverId = apiQuote.id,
                    selectedText = apiQuote.selectedText,
                    positionStart = apiQuote.positionStart,
                    positionEnd = apiQuote.positionEnd,
                    bookId = booksDao.getBookByServerId(apiQuote.book.id)?.id ?: 0,
                    bookTitle = apiQuote.book.title,
                    noteContent = apiQuote.noteContent,
                    isSynced = true,
                    isDeleted = false
                )
            }.filter { it.bookId != 0 }

            quotesDao.insertQuotes(entities)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun refreshGroupedQuotes(): TResult<Unit, ConnectionExceptionDomainModel> {
        return refreshQuotes()
    }

    override suspend fun createQuote(
        bookId: Int,
        text: String,
        start: Int,
        end: Int,
        note: String?,
        locatorData: String?
    ): TResult<Unit, ConnectionExceptionDomainModel> {
        val bookEntity = booksDao.getBookEntityById(bookId)
            ?: return TResult.Error(Exception("Book not found locally").toConnectionExceptionDomainModel())

        val newQuote = QuoteEntity(
            selectedText = text,
            positionStart = start,
            positionEnd = end,
            bookId = bookId,
            bookTitle = bookEntity.title,
            noteContent = note,
            locatorData = locatorData,
            isSynced = false,
            serverId = null
        )

        val localQuoteId = quotesDao.insertQuote(newQuote).toInt()

        if (bookEntity.serverId != null) {
            try {
                val request = CreateQuoteRequestApiModel(
                    selectedText = text,
                    positionStart = start,
                    positionEnd = end,
                    note = note,
                    locatorData = locatorData
                )
                val apiResponse = quotesApi.createQuote(bookEntity.serverId, request)

                val syncedQuote = newQuote.copy(
                    id = localQuoteId,
                    serverId = apiResponse.id,
                    isSynced = true
                )
                quotesDao.insertQuote(syncedQuote)
            } catch (e: Exception) {
            }
        }

        return TResult.Success(Unit)
    }

    override suspend fun deleteQuote(quoteId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val quote = quotesDao.getQuoteEntityById(quoteId)
                ?: return TResult.Error(Exception("Quote not found").toConnectionExceptionDomainModel())

            if (quote.serverId == null) {
                quotesDao.deleteQuotePhysically(quoteId)
            } else {
                quotesDao.markAsDeleted(quoteId)

                try {
                    quotesApi.deleteQuote(quote.serverId)
                    quotesDao.deleteQuotePhysically(quoteId)
                } catch (e: Exception) {
                }
            }
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}