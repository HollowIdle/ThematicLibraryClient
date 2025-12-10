package com.example.thematiclibraryclient.data.remote.model.bookmarks

import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.google.gson.annotations.SerializedName

data class BookmarkApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("position")
    val position: Int,

    @SerializedName("note")
    val note: String?
)

fun BookmarkApiModel.toDomainModel() = BookmarkDomainModel(
    id = this.id,
    position = this.position,
    note = this.note
)