package com.example.thematiclibraryclient.data.remote.model.quotes

import com.example.thematiclibraryclient.data.remote.model.notes.NoteApiModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.google.gson.annotations.SerializedName

data class QuoteApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("selectedText")
    val selectedText: String,

    @SerializedName("positionStart")
    val positionStart: Int,

    @SerializedName("positionEnd")
    val positionEnd: Int,

    @SerializedName("note")
    val note: NoteApiModel?
)

fun QuoteApiModel.toDomainModel(bookId: Int, bookTitle: String = "") = QuoteDomainModel(
    id = this.id,
    selectedText = this.selectedText,
    positionStart = this.positionStart,
    positionEnd = this.positionEnd,
    bookId = bookId,
    bookTitle = bookTitle,
    noteContent = note?.content
)