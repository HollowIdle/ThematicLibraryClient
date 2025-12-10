package com.example.thematiclibraryclient.data.remote.model.books

import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.google.gson.annotations.SerializedName

data class BookDetailsApiModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("authors")
    val authors: List<String>,
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("shelfIds")
    val shelfIds: List<Int>
)

fun BookDetailsApiModel.toDomainModel() = BookDetailsDomainModel(
    id = this.id,
    title = this.title,
    description = this.description,
    authors = this.authors,
    tags = this.tags,
    shelfIds = this.shelfIds
)