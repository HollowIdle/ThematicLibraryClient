package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel

interface IShelvesRemoteRepository {

    suspend fun getShelves(): TResult<List<ShelfDomainModel>, ConnectionExceptionDomainModel>

    suspend fun createShelf(name: String): TResult<ShelfDomainModel, ConnectionExceptionDomainModel>

    suspend fun updateShelf(shelfId: Int, newName: String): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun deleteShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun addBookToShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun removeBookFromShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel>
}