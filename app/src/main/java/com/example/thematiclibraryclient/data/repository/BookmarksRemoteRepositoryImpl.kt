package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.BookmarksDao
import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.entity.BookmarkEntity
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBookmarksApi
import com.example.thematiclibraryclient.data.remote.model.bookmarks.CreateBookmarkRequestApiModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookmarksRemoteRepositoryImpl @Inject constructor(
    private val bookmarksApi: IBookmarksApi,
    private val bookmarksDao: BookmarksDao,
    private val booksDao: BooksDao
) : IBookmarksRemoteRepository {

    override fun getBookmarks(bookId: Int): Flow<List<BookmarkDomainModel>> {
        return bookmarksDao.getBookmarksForBook(bookId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun refreshBookmarks(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        val localBook = booksDao.getBookEntityById(bookId)
        if (localBook?.serverId == null) return TResult.Success(Unit)

        return try {
            val apiModels = bookmarksApi.getBookmarksForBook(localBook.serverId)
            val entities = apiModels.map { apiModel ->
                val existing = bookmarksDao.getBookmarkByServerId(apiModel.id)
                BookmarkEntity(
                    id = existing?.id ?: 0,
                    serverId = apiModel.id,
                    position = apiModel.position,
                    note = apiModel.note,
                    bookId = bookId,
                    isSynced = true,
                    isDeleted = false
                )
            }
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
        val newBookmark = BookmarkEntity(
            position = position,
            note = note,
            bookId = bookId,
            isSynced = false,
            serverId = null
        )
        val localId = bookmarksDao.insertBookmark(newBookmark).toInt()

        val book = booksDao.getBookEntityById(bookId)
        if (book?.serverId != null) {
            try {
                val request = CreateBookmarkRequestApiModel(position, note)
                val response = bookmarksApi.createBookmark(book.serverId, request)

                val syncedBookmark = newBookmark.copy(
                    id = localId,
                    serverId = response.id,
                    isSynced = true
                )
                bookmarksDao.insertBookmark(syncedBookmark)
            } catch (e: Exception) { }
        }
        return TResult.Success(Unit)
    }

    override suspend fun deleteBookmark(bookmarkId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        val bookmark = bookmarksDao.getBookmarkEntityById(bookmarkId)
            ?: return TResult.Error(Exception("Bookmark not found").toConnectionExceptionDomainModel())

        if (bookmark.serverId == null) {
            bookmarksDao.deleteBookmarkPhysically(bookmarkId)
        } else {
            bookmarksDao.markAsDeleted(bookmarkId)
            try {
                bookmarksApi.deleteBookmark(bookmark.serverId)
                bookmarksDao.deleteBookmarkPhysically(bookmarkId)
            } catch (e: Exception) { }
        }
        return TResult.Success(Unit)
    }

    override suspend fun syncPendingChanges(): TResult<Unit, Exception> {
        return try {
            val deleted = bookmarksDao.getDeletedBookmarks()
            deleted.forEach { bm ->
                if (bm.serverId != null) {
                    try { bookmarksApi.deleteBookmark(bm.serverId) } catch (e: Exception) {}
                }
                bookmarksDao.deleteBookmarkPhysically(bm.id)
            }

            val unsynced = bookmarksDao.getUnsyncedBookmarks()
            for (bm in unsynced) {
                val book = booksDao.getBookEntityById(bm.bookId)
                if (book?.serverId == null) continue

                if (bm.serverId == null) {
                    val request = CreateBookmarkRequestApiModel(bm.position, bm.note)
                    val response = bookmarksApi.createBookmark(book.serverId, request)
                    bookmarksDao.insertBookmark(bm.copy(serverId = response.id, isSynced = true))
                }
            }
            TResult.Success(Unit)
        } catch (e: Exception) {
            TResult.Error(e)
        }
    }

}