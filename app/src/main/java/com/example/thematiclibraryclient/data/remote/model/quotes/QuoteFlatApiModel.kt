package com.example.thematiclibraryclient.data.remote.model.quotes

import com.example.thematiclibraryclient.domain.model.quotes.QuoteFlatDomainModel
import com.google.gson.annotations.SerializedName

data class QuoteFlatApiModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("selectedText")
    val selectedText: String,
    @SerializedName("positionStart")
    val positionStart: Int,
    @SerializedName("positionEnd")
    val positionEnd: Int,
    @SerializedName("book")
    val book: BookInQuoteApiModel,
    @SerializedName("noteContent")
    val noteContent: String?
)

data class BookInQuoteApiModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String
)

data class NoteInQuoteApiModel(
    val content: String?
)

fun QuoteFlatApiModel.toDomainModel() = QuoteFlatDomainModel(
    id = this.id,
    selectedText = this.selectedText,
    positionStart = this.positionStart,
    bookId = this.book.id,
    bookTitle = this.book.title,
    noteContent = noteContent
)