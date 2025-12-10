package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.bookmarks.BookmarkApiModel
import com.example.thematiclibraryclient.data.remote.model.bookmarks.CreateBookmarkRequestApiModel
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface IBookmarksApi {

    @GET("/api/books/{bookId}/bookmarks")
    suspend fun getBookmarksForBook(
        @Path("bookId") bookId: Int
    ) : List<BookmarkApiModel>

    @POST("/api/books/{bookId}/bookmarks")
    suspend fun createBookmark(
        @Path("bookId") bookId: Int,
        @Body request: CreateBookmarkRequestApiModel
    ): BookmarkApiModel

    @DELETE("/api/bookmarks/{bookmarkId}")
    suspend fun deleteBookmark(@Path("bookmarkId") bookmarkId: Int)

}