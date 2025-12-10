package com.example.thematiclibraryclient.data.remote.model.grouped

import com.example.thematiclibraryclient.domain.model.quotes.BookGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.google.gson.annotations.SerializedName

data class ShelfGroupApiModel(
    @SerializedName("shelfId")
    val shelfId: Int,

    @SerializedName("shelfName")
    val shelfName: String,

    @SerializedName("books")
    val books: List<BookGroupApiModel>
)

data class BookGroupApiModel(
    @SerializedName("bookId")
    val bookId: Int,

    @SerializedName("bookTitle")
    val bookTitle: String,

    @SerializedName("quotes")
    val quotes: List<QuoteGroupApiModel>
)

data class QuoteGroupApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("selectedText")
    val selectedText: String
)

fun BookGroupApiModel.toDomainModel() = BookGroupDomainModel(
    bookId = this.bookId,
    bookTitle = this.bookTitle,
    quotes = this.quotes.map { it.toDomainModel() }
)

fun ShelfGroupApiModel.toDomainModel() = ShelfGroupDomainModel(
    shelfId = this.shelfId,
    shelfName = this.shelfName,
    books = this.books.map { it.toDomainModel() }
)

fun QuoteGroupApiModel.toDomainModel() = QuoteGroupDomainModel(
    id = this.id,
    selectedText = this.selectedText
)
