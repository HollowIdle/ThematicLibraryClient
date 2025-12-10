package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.shelves.ShelfListItemApiModel
import com.example.thematiclibraryclient.data.remote.model.shelves.ShelfRequestApiModel
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IShelvesApi {
    @GET("/api/shelves")
    suspend fun getShelves(): List<ShelfListItemApiModel>

    @POST("/api/shelves")
    suspend fun createShelf(@Body request: ShelfRequestApiModel): ShelfListItemApiModel

    @PUT("/api/shelves/{id}")
    suspend fun updateShelf(@Path("id") shelfId: Int, @Body request: ShelfRequestApiModel)

    @DELETE("/api/shelves/{id}")
    suspend fun deleteShelf(@Path("id") shelfId: Int)

    @POST("/api/shelves/{shelfId}/books/{bookId}")
    suspend fun addBookToShelf(
        @Path("shelfId") shelfId: Int,
        @Path("bookId") bookId: Int
    )

    @DELETE("/api/shelves/{shelfId}/books/{bookId}")
    suspend fun removeBookFromShelf(
        @Path("shelfId") shelfId: Int,
        @Path("bookId") bookId: Int
    )
}