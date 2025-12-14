package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.grouped.ShelfGroupApiModel
import com.example.thematiclibraryclient.data.remote.model.quotes.CreateQuoteRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.quotes.QuoteApiModel
import com.example.thematiclibraryclient.data.remote.model.quotes.QuoteFlatApiModel
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface IQuotesApi {

    @GET("/api/quotes/grouped")
    suspend fun getGroupedQuotes(): List<ShelfGroupApiModel>

    @GET("/api/quotes")
    suspend fun getFlatQuotes(): List<QuoteFlatApiModel>

    @GET("/api/books/{bookId}/quotes")
    suspend fun getQuotesForBook(
        @Path("bookId") bookId: Int
    ): List<QuoteApiModel>

    @POST("/api/books/{bookId}/quotes")
    suspend fun createQuote(
        @Path("bookId") bookId: Int,
        @Body request: CreateQuoteRequestApiModel
    ): QuoteApiModel

    @DELETE("/api/quotes/{id}")
    suspend fun deleteQuote(@Path("id") quoteId: Int)
}