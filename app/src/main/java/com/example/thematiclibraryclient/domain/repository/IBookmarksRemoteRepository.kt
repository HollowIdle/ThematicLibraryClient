package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel

interface IBookmarksRemoteRepository {
    suspend fun createBookmark(bookId: Int, position: Int, note: String?) :
            TResult<BookmarkDomainModel, ConnectionExceptionDomainModel>

    suspend fun getBookmarksForBook(bookId: Int) :
            TResult<List<BookmarkDomainModel>, ConnectionExceptionDomainModel>

    suspend fun deleteBookmark(bookmarkId: Int): TResult<Unit, ConnectionExceptionDomainModel>
}