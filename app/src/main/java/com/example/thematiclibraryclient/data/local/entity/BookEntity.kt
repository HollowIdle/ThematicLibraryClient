package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val title: String,
    val description: String?,
    val authors: List<AuthorDomainModel>,
    val tags: List<String>,
    val shelfIds: List<Int>,
    val lastPosition: Int = 0,
    val isDetailsLoaded: Boolean = false
)

fun BookEntity.toDomainModel() = BookDomainModel(
    id = id,
    title = title,
    description = description,
    authors = authors,
    tags = tags,
    shelfIds = shelfIds
)

fun BookEntity.toDetailsDomainModel() = BookDetailsDomainModel(
    id = id,
    title = title,
    description = description,
    authors = authors.map { it.name },
    tags = tags,
    shelfIds = shelfIds,
    lastPosition = lastPosition
)