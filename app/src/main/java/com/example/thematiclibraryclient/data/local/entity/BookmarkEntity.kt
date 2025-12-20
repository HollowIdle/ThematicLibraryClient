package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int? = null,
    val position: Int,
    val note: String?,
    val bookId: Int,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun BookmarkEntity.toDomainModel() = BookmarkDomainModel(
    id = this.id,
    position = this.position,
    note = this.note
)