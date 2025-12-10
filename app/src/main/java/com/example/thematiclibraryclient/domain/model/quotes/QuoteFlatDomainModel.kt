package com.example.thematiclibraryclient.domain.model.quotes

data class QuoteFlatDomainModel(
    val id: Int,
    val selectedText: String,
    val positionStart: Int,
    val bookId: Int,
    val bookTitle: String,
    val noteContent: String?
)