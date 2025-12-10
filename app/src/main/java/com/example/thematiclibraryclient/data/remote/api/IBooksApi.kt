package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.books.BookDetailsApiModel
import com.example.thematiclibraryclient.data.remote.model.books.BookListItemApiModel
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface IBooksApi {

    @GET("/api/books")
    suspend fun getBooks(): List<BookListItemApiModel>

    @GET("/api/books/{id}")
    suspend fun getBookDetails(@Path("id") bookId: Int): BookDetailsApiModel

    @GET("/api/books/{id}/content")
    suspend fun getBookContent(@Path("id") bookId: Int): ResponseBody

    @Multipart
    @POST("/api/books/upload")
    suspend fun uploadBook(
        @Part file: MultipartBody.Part
    ): BookListItemApiModel

}