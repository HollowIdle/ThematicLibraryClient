package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBookmarksApi
import com.example.thematiclibraryclient.data.remote.model.bookmarks.CreateBookmarkRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.bookmarks.toDomainModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import jakarta.inject.Inject

class BookmarksRemoteRepositoryImpl @Inject constructor(
    private val bookmarksApi: IBookmarksApi
) : IBookmarksRemoteRepository {
    override suspend fun createBookmark(bookId: Int, position: Int, note: String?): TResult<BookmarkDomainModel, ConnectionExceptionDomainModel> {
        return try {
            val request = CreateBookmarkRequestApiModel(position, note)
            val response = bookmarksApi.createBookmark(bookId, request)
            TResult.Success(response.toDomainModel())
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun getBookmarksForBook(bookId: Int): TResult<List<BookmarkDomainModel>, ConnectionExceptionDomainModel> {
        return try {
            val response = bookmarksApi.getBookmarksForBook(bookId)
            TResult.Success(response.map { it.toDomainModel() })
        } catch (e: Throwable){
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun deleteBookmark(bookmarkId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            bookmarksApi.deleteBookmark(bookmarkId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}