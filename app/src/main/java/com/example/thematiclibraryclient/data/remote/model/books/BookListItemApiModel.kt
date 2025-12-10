package com.example.thematiclibraryclient.data.remote.model.books

import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.google.gson.annotations.SerializedName

data class BookListItemApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("authors")
    val authors: List<AuthorApiModel>
)

fun BookListItemApiModel.toDomainModel() =
    BookDomainModel(
        id = id,
        title = title,
        description = description,
        authors = authors?.map { it -> it.toDomainModel() } ?: emptyList()
    )
