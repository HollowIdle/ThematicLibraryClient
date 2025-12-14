package com.example.thematiclibraryclient.domain.repository

import android.net.Uri
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import kotlinx.coroutines.flow.Flow

interface IBooksRemoteRepository {

    val booksUpdateFlow: Flow<Unit>
    suspend fun getBooks(): TResult<List<BookDomainModel>,
            ConnectionExceptionDomainModel>
    suspend fun getBookContent(bookId: Int): TResult<String, ConnectionExceptionDomainModel>

    suspend fun getBookDetails(bookId: Int): TResult<BookDetailsDomainModel, ConnectionExceptionDomainModel>
    suspend fun uploadBook(fileUri: Uri) : TResult<BookDomainModel, ConnectionExceptionDomainModel>

    suspend fun updateAuthors(bookId: Int, authors: List<String>): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun updateTags(bookId: Int, tags: List<String>): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun updateDescription(bookId: Int, description: String): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun updateProgress(bookId: Int, position: Int): TResult<Unit, ConnectionExceptionDomainModel>
    suspend fun deleteBook(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>
}