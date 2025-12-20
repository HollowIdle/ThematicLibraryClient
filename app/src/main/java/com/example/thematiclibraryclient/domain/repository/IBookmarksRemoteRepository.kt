package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import kotlinx.coroutines.flow.Flow

interface IBookmarksRemoteRepository {

    fun getBookmarks(bookId: Int): Flow<List<BookmarkDomainModel>>

    suspend fun refreshBookmarks(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun createBookmark(bookId: Int, position: Int, note: String?): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun deleteBookmark(bookmarkId: Int): TResult<Unit, ConnectionExceptionDomainModel>
}