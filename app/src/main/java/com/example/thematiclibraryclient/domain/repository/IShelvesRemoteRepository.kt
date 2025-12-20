package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import kotlinx.coroutines.flow.Flow

interface IShelvesRemoteRepository {

    fun getShelves(): Flow<List<ShelfDomainModel>>

    fun getBooksOnShelf(shelfId: Int): Flow<List<BookDomainModel>>

    suspend fun refreshShelves(): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun refreshBooksOnShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun createShelf(name: String): TResult<Unit, ConnectionExceptionDomainModel>
    suspend fun updateShelf(shelfId: Int, name: String): TResult<Unit, ConnectionExceptionDomainModel>
    suspend fun deleteShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun addBookToShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>
    suspend fun removeBookFromShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>
}