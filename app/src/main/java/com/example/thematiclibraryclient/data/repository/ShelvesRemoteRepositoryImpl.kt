package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IShelvesApi
import com.example.thematiclibraryclient.data.remote.model.shelves.ShelfRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.shelves.toDomainModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import jakarta.inject.Inject

class ShelvesRemoteRepositoryImpl @Inject constructor(
    private val shelvesApi: IShelvesApi
) : IShelvesRemoteRepository {

    override suspend fun getShelves(): TResult<List<ShelfDomainModel>, ConnectionExceptionDomainModel> {
        return try {
            val shelvesApiModel = shelvesApi.getShelves()
            TResult.Success(shelvesApiModel.map { it.toDomainModel() })
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun createShelf(name: String): TResult<ShelfDomainModel, ConnectionExceptionDomainModel> {
        return try {
            val request = ShelfRequestApiModel(name = name)
            val createdShelfApiModel = shelvesApi.createShelf(request)
            TResult.Success(createdShelfApiModel.toDomainModel())
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateShelf(shelfId: Int, newName: String): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val request = ShelfRequestApiModel(name = newName)
            shelvesApi.updateShelf(shelfId, request)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun deleteShelf(shelfId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.deleteShelf(shelfId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun addBookToShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.addBookToShelf(shelfId, bookId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun removeBookFromShelf(shelfId: Int, bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            shelvesApi.removeBookFromShelf(shelfId, bookId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}