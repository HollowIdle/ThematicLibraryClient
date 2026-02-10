package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.books.BookDetailsApiModel
import com.example.thematiclibraryclient.data.remote.model.books.BookListItemApiModel
import com.example.thematiclibraryclient.data.remote.model.books.BookProgressRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.books.UpdateDescriptionRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.books.UploadBookResponseApiModel
import com.example.thematiclibraryclient.data.remote.model.common.StringListRequestApiModel
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface IBooksApi {

    @GET("/api/books")
    suspend fun getBooks(): List<BookListItemApiModel>

    @GET("/api/books/{id}")
    suspend fun getBookDetails(@Path("id") bookId: Int): BookDetailsApiModel

    /*
    @GET("/api/books/{id}/content")
    suspend fun getBookContent(@Path("id") bookId: Int): ResponseBody
    */

    @Streaming
    @GET("/api/books/{id}/download")
    suspend fun downloadBook(@Path("id") bookId: Int): Response<ResponseBody>

    @Multipart
    @POST("/api/books/upload")
    suspend fun uploadBook(
        @Part file: MultipartBody.Part
    ): UploadBookResponseApiModel

    @POST("/api/books/{bookId}/description")
    suspend fun updateDescription(
        @Path("bookId") bookId: Int,
        @Body request: UpdateDescriptionRequestApiModel
    )

    @POST("/api/books/{bookId}/authors")
    suspend fun updateAuthors(
        @Path("bookId") bookId: Int,
        @Body request: StringListRequestApiModel
    )

    @POST("/api/books/{bookId}/tags")
    suspend fun updateTags(
        @Path("bookId") bookId: Int,
        @Body request: StringListRequestApiModel
    )

    @POST("/api/books/{bookId}/progress")
    suspend fun updateProgress(
        @Path("bookId") bookId: Int,
        @Body request: BookProgressRequestApiModel
    )

    @DELETE("/api/books/{id}")
    suspend fun deleteBook(@Path("id") bookId: Int)

}