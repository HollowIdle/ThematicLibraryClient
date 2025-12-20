package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val selectedText: String,
    val positionStart: Int,
    val positionEnd: Int,
    val bookId: Int,
    val bookTitle: String,
    val noteContent: String?,
    val locatorData: String? = null
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