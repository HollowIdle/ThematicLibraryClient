package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.dao.ShelvesDao
import com.example.thematiclibraryclient.data.local.entity.ShelfEntity
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IShelvesApi
import com.example.thematiclibraryclient.data.remote.model.books.toEntity
import com.example.thematiclibraryclient.data.remote.model.shelves.ShelfRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.shelves.toEntity
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShelvesRemoteRepositoryImpl @Inject constructor(
    private val shelvesApi: IShelvesApi,
    private val shelvesDao: ShelvesDao,
    private val booksDao: BooksDao
) : IShelvesRemoteRepository {

    override fun getShelves(): Flow<List<ShelfDomainModel>> {
        return shelvesDao.getShelves().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getBooksOnShelf(shelfId: Int): Flow<List<BookDomainModel>> {
        return booksDao.getBooks().map { allBooks ->
            allBooks
                .filter { it.shelfIds.contains(shelfId) }
                .map { it.toDomainModel() }
        }
    }

    override suspend fun refreshShelves(): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiShelves = shelvesApi.getShelves()
            shelvesDao.insertShelves(apiShelves.map { it.toEntity() })
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun refreshBooksOnShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiBooks = shelvesApi.getBooksOnShelf(shelfId)
            booksDao.insertBooks(apiBooks.map { it.toEntity() })
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun createShelf(name: String): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val newShelfApi = shelvesApi.createShelf(ShelfRequestApiModel(name))
            shelvesDao.insertShelf(newShelfApi.toEntity())
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateShelf(shelfId: Int, name: String): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.updateShelf(shelfId, ShelfRequestApiModel(name))
            shelvesDao.insertShelf(ShelfEntity(shelfId, name))
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun deleteShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.deleteShelf(shelfId)
            shelvesDao.deleteShelf(shelfId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun addBookToShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.addBookToShelf(shelfId, bookId)

            updateLocalBookShelves(bookId) { currentIds -> currentIds + shelfId }

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun removeBookFromShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.removeBookFromShelf(shelfId, bookId)

            updateLocalBookShelves(bookId) { currentIds -> currentIds - shelfId }

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    private suspend fun updateLocalBookShelves(bookId: Int, transform: (List<Int>) -> List<Int>) {
        val bookEntity = booksDao.getBookEntityById(bookId)

        if (bookEntity != null) {
            val newShelfIds = transform(bookEntity.shelfIds).distinct()

            android.util.Log.d("ShelvesRepo", "Updating book $bookId shelves: ${bookEntity.shelfIds} -> $newShelfIds")

            val updatedBook = bookEntity.copy(shelfIds = newShelfIds)
            booksDao.insertBook(updatedBook)
        } else {
            android.util.Log.e("ShelvesRepo", "Book $bookId not found in local DB during shelf update")
        }
    }
}