package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.dao.ShelvesDao
import com.example.thematiclibraryclient.data.local.entity.BookEntity
import com.example.thematiclibraryclient.data.local.entity.ShelfEntity
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBooksApi
import com.example.thematiclibraryclient.data.remote.api.IShelvesApi
import com.example.thematiclibraryclient.data.remote.model.books.toEntity
import com.example.thematiclibraryclient.data.remote.model.shelves.ShelfRequestApiModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShelvesRemoteRepositoryImpl @Inject constructor(
    private val shelvesApi: IShelvesApi,
    private val booksApi: IBooksApi,
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
            val entities = apiShelves.map { apiShelf ->
                val existing = shelvesDao.getShelfByServerId(apiShelf.id)
                ShelfEntity(
                    id = existing?.id ?: 0,
                    serverId = apiShelf.id,
                    name = apiShelf.name,
                    isSynced = true,
                    isDeleted = false
                )
            }
            shelvesDao.insertShelves(entities)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun refreshBooksOnShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        val localShelf = shelvesDao.getShelfEntityById(shelfId)
            ?: return TResult.Error(Exception("Shelf not found locally").toConnectionExceptionDomainModel())

        if (localShelf.serverId == null) return TResult.Success(Unit)

        return try {
            val apiBooks = shelvesApi.getBooksOnShelf(localShelf.serverId)
            val apiBookServerIds = apiBooks.map { it.id }.toSet()

            val booksToInsert = mutableListOf<BookEntity>()
            val booksToUpdate = mutableListOf<BookEntity>()

            for (apiBook in apiBooks) {
                val existingBook = booksDao.getBookByServerId(apiBook.id)

                if (existingBook != null) {
                    if (!existingBook.shelfIds.contains(shelfId)) {
                        val newShelfIds = existingBook.shelfIds + shelfId
                        booksToUpdate.add(existingBook.copy(shelfIds = newShelfIds, isSynced = true))
                    } else {
                        if (!existingBook.isSynced) {
                            booksToUpdate.add(existingBook.copy(isSynced = true))
                        }
                    }
                } else {
                    val newBook = apiBook.toEntity().copy(
                        id = 0,
                        shelfIds = listOf(shelfId),
                        isSynced = true
                    )
                    booksToInsert.add(newBook)
                }
            }

            val localBooksOnShelf = booksDao.getBooksByShelfId(shelfId).filter { it.shelfIds.contains(shelfId) }

            for (localBook in localBooksOnShelf) {
                if (localBook.serverId != null && !apiBookServerIds.contains(localBook.serverId)) {
                    val newShelfIds = localBook.shelfIds - shelfId
                    booksToUpdate.add(localBook.copy(shelfIds = newShelfIds, isSynced = true))
                }
            }

            if (booksToInsert.isNotEmpty()) {
                booksDao.insertBooks(booksToInsert)
            }

            booksToUpdate.forEach {
                booksDao.updateBook(it)
            }

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun createShelf(name: String): TResult<Unit, ConnectionExceptionDomainModel> {
        val newShelf = ShelfEntity(
            name = name,
            isSynced = false,
            serverId = null
        )
        val localId = shelvesDao.insertShelf(newShelf).toInt()

        try {
            val apiShelf = shelvesApi.createShelf(ShelfRequestApiModel(name))
            val syncedShelf = newShelf.copy(
                id = localId,
                serverId = apiShelf.id,
                isSynced = true
            )
            shelvesDao.insertShelf(syncedShelf)
        } catch (e: Exception) {
        }
        return TResult.Success(Unit)
    }

    override suspend fun updateShelf(shelfId: Int, name: String): TResult<Unit, ConnectionExceptionDomainModel> {
        val existing = shelvesDao.getShelfEntityById(shelfId)
            ?: return TResult.Error(Exception("Shelf not found").toConnectionExceptionDomainModel())

        val updated = existing.copy(name = name, isSynced = false)
        shelvesDao.insertShelf(updated)

        if (existing.serverId != null) {
            try {
                shelvesApi.updateShelf(existing.serverId, ShelfRequestApiModel(name))
                shelvesDao.insertShelf(updated.copy(isSynced = true))
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    override suspend fun deleteShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        val shelf = shelvesDao.getShelfEntityById(shelfId)
            ?: return TResult.Error(Exception("Shelf not found").toConnectionExceptionDomainModel())

        if (shelf.serverId == null) {
            shelvesDao.deleteShelfPhysically(shelfId)
        } else {
            shelvesDao.markAsDeleted(shelfId)

            try {
                shelvesApi.deleteShelf(shelf.serverId)
                shelvesDao.deleteShelfPhysically(shelfId)
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    override suspend fun addBookToShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        updateLocalBookShelves(bookId) { currentIds -> currentIds + shelfId }

        val book = booksDao.getBookEntityById(bookId)
        val shelf = shelvesDao.getShelfEntityById(shelfId)

        if (book?.serverId != null && shelf?.serverId != null) {
            try {
                shelvesApi.addBookToShelf(shelf.serverId, book.serverId)
                val updatedBook = booksDao.getBookEntityById(bookId)
                if (updatedBook != null) {
                    booksDao.updateBook(updatedBook.copy(isSynced = true))
                }
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    override suspend fun removeBookFromShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        updateLocalBookShelves(bookId) { currentIds -> currentIds - shelfId }

        val book = booksDao.getBookEntityById(bookId)
        val shelf = shelvesDao.getShelfEntityById(shelfId)

        if (book?.serverId != null && shelf?.serverId != null) {
            try {
                shelvesApi.removeBookFromShelf(shelf.serverId, book.serverId)
                val updatedBook = booksDao.getBookEntityById(bookId)
                if (updatedBook != null) {
                    booksDao.updateBook(updatedBook.copy(isSynced = true))
                }
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    private suspend fun updateLocalBookShelves(bookId: Int, transform: (List<Int>) -> List<Int>) {
        val bookEntity = booksDao.getBookEntityById(bookId)
        if (bookEntity != null) {
            val newShelfIds = transform(bookEntity.shelfIds).distinct()
            val updatedBook = bookEntity.copy(shelfIds = newShelfIds, isSynced = false)

            booksDao.updateBook(updatedBook)
        }
    }

    override suspend fun syncPendingChanges(): TResult<Unit, Exception> {
        // Используем переменную для отслеживания глобального успеха,
        // но не прерываем выполнение из-за одной ошибки
        var hasError = false

        try {
            // 1. Удаление полок
            val deletedShelves = shelvesDao.getDeletedShelves()
            deletedShelves.forEach { shelf ->
                if (shelf.serverId != null) {
                    try { shelvesApi.deleteShelf(shelf.serverId) } catch (e: Exception) { hasError = true }
                }
                shelvesDao.deleteShelfPhysically(shelf.id)
            }

            // 2. Создание/Обновление полок
            val unsyncedShelves = shelvesDao.getUnsyncedShelves()
            for (shelf in unsyncedShelves) {
                try {
                    if (shelf.serverId == null) {
                        val response = shelvesApi.createShelf(ShelfRequestApiModel(shelf.name))
                        shelvesDao.insertShelf(shelf.copy(serverId = response.id, isSynced = true))
                    } else {
                        shelvesApi.updateShelf(shelf.serverId, ShelfRequestApiModel(shelf.name))
                        shelvesDao.insertShelf(shelf.copy(isSynced = true))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_LOG", "Ошибка полки ${shelf.name}", e)
                    hasError = true
                }
            }

            // 3. Синхронизация связей Книга-Полка (САМОЕ ВАЖНОЕ ИСПРАВЛЕНИЕ)
            val unsyncedBooks = booksDao.getUnsyncedBooks()

            for (localBook in unsyncedBooks) {
                // Пропускаем, если книга еще не загружена на сервер
                if (localBook.serverId == null || localBook.serverId == 0) continue

                try {
                    // Получаем текущее состояние полок книги на сервере
                    val serverBookDetails = booksApi.getBookDetails(localBook.serverId)
                    val serverShelfIds = serverBookDetails.shelfIds.toSet()

                    // Вычисляем, какие полки есть локально
                    val localShelfServerIds = localBook.shelfIds.mapNotNull { localShelfId ->
                        shelvesDao.getShelfEntityById(localShelfId)?.serverId
                    }.toSet()

                    val toAdd = localShelfServerIds - serverShelfIds
                    val toRemove = serverShelfIds - localShelfServerIds

                    toAdd.forEach { shelfServerId ->
                        shelvesApi.addBookToShelf(shelfServerId, localBook.serverId)
                    }
                    toRemove.forEach { shelfServerId ->
                        shelvesApi.removeBookFromShelf(shelfServerId, localBook.serverId)
                    }

                    android.util.Log.d("SYNC_LOG", "Полки книги ${localBook.title} синхронизированы. isSynced=true")

                    booksDao.updateBook(localBook.copy(isSynced = true))

                } catch (e: Exception) {
                    android.util.Log.e("SYNC_LOG", "Ошибка связей книги ${localBook.title}", e)
                    hasError = true
                }
            }

            return if (hasError) TResult.Error(Exception("Completed with errors")) else TResult.Success(Unit)

        } catch (e: Exception) {
            return TResult.Error(e)
        }
    }

}