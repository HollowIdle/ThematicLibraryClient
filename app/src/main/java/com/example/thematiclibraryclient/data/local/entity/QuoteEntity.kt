package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel

@Entity(
    tableName = "quotes",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int? = null,
    val selectedText: String,
    val positionStart: Int,
    val positionEnd: Int,
    val bookId: Int,
    val bookTitle: String,
    val noteContent: String?,
    val locatorData: String? = null,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

fun QuoteEntity.toDomainModel() = QuoteDomainModel(
    id = id,
    selectedText = selectedText,
    positionStart = positionStart,
    positionEnd = positionEnd,
    bookId = bookId,
    bookTitle = bookTitle,
    noteContent = noteContent,
    locatorData = locatorData
)