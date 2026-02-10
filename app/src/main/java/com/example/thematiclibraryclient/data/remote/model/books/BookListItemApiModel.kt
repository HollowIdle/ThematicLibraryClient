package com.example.thematiclibraryclient.data.remote.model.books

import com.example.thematiclibraryclient.data.local.entity.BookEntity
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
    val authors: List<AuthorApiModel>,

    @SerializedName("tags")
    val tags: List<String>,

    @SerializedName("shelfIds")
    val shelfIds: List<Int>,
)

fun BookListItemApiModel.toDomainModel() =
    BookDomainModel(
        id = id,
        title = title,
        description = description,
        authors = authors.map { it -> it.toDomainModel() },
        tags = tags,
        shelfIds = shelfIds
    )

fun BookListItemApiModel.toEntity() = BookEntity(
    id = 0,
    serverId = this.id,
    title = this.title,
    description = this.description,
    authors = this.authors.map { it.toDomainModel() },
    tags = this.tags,
    shelfIds = this.shelfIds,
    lastPosition = 0,
    isDetailsLoaded = false,
    isSynced = true
)
