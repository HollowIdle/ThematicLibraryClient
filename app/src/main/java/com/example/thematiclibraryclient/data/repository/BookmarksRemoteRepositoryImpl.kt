package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.BookmarksDao
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBookmarksApi
import com.example.thematiclibraryclient.data.remote.model.bookmarks.CreateBookmarkRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.bookmarks.toEntity
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookmarksRemoteRepositoryImpl @Inject constructor(
    private val bookmarksApi: IBookmarksApi,
    private val bookmarksDao: BookmarksDao
) : IBookmarksRemoteRepository {

    override fun getBookmarks(bookId: Int): Flow<List<BookmarkDomainModel>> {
        return bookmarksDao.getBookmarksForBook(bookId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun refreshBookmarks(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiModels = bookmarksApi.getBookmarksForBook(bookId)
            val entities = apiModels.map { it.toEntity(bookId) }
            bookmarksDao.insertBookmarks(entities)

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun createBookmark(
        bookId: Int,
        position: Int,
        note: String?
    ): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val request = CreateBookmarkRequestApiModel(position, note)
            val response = bookmarksApi.createBookmark(bookId, request)

            bookmarksDao.insertBookmark(response.toEntity(bookId))

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun deleteBookmark(bookmarkId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            bookmarksApi.deleteBookmark(bookmarkId)

            bookmarksDao.deleteBookmarkPhysically(bookmarkId)

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}