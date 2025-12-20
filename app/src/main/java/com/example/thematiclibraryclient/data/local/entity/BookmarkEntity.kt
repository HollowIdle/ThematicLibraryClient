package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val position: Int,
    val note: String?,
    val bookId: Int
)

fun BookmarkEntity.toDomainModel() = BookmarkDomainModel(
    id = this.id,
    position = this.position,
    note = this.note
)