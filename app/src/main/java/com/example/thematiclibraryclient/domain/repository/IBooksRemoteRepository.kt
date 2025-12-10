package com.example.thematiclibraryclient.domain.repository

import android.net.Uri
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel

interface IBooksRemoteRepository {
    suspend fun getBooks(): TResult<List<BookDomainModel>,
            ConnectionExceptionDomainModel>
    suspend fun getBookContent(bookId: Int): TResult<String, ConnectionExceptionDomainModel>

    suspend fun getBookDetails(bookId: Int): TResult<BookDetailsDomainModel, ConnectionExceptionDomainModel>
    suspend fun uploadBook(fileUri: Uri) : TResult<BookDomainModel, ConnectionExceptionDomainModel>

}