package com.example.thematiclibraryclient.domain.repository

import android.net.Uri
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import kotlinx.coroutines.flow.Flow
import java.io.File

interface IBooksRemoteRepository {


    fun getBooks(): Flow<List<BookDomainModel>>

    fun getBookDetails(bookId: Int): Flow<BookDetailsDomainModel?>

    suspend fun refreshBooks(): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun refreshBookDetails(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun uploadBook(fileUri: Uri): TResult<Unit, ConnectionExceptionDomainModel> // Изменили возвращаемый тип на Unit, так как список обновится через Flow

    suspend fun deleteBook(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun updateAuthors(bookId: Int, authors: List<String>): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun updateTags(bookId: Int, tags: List<String>): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun updateDescription(bookId: Int, description: String): TResult<Unit, ConnectionExceptionDomainModel>

    /*
    suspend fun getBookContent(bookId: Int): TResult<String, ConnectionExceptionDomainModel>
    */

    suspend fun downloadBookFile(bookId: Int, fileName: String): TResult<File, ConnectionExceptionDomainModel>

    suspend fun updateProgress(bookId: Int, position: Int): TResult<Unit, ConnectionExceptionDomainModel>

    val booksUpdateFlow: Flow<Unit>
}