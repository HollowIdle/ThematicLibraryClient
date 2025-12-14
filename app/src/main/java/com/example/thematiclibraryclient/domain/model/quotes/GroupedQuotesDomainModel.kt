package com.example.thematiclibraryclient.domain.model.quotes

data class ShelfGroupDomainModel(
    val shelfId: Int,
    val shelfName: String,
    val books: List<BookGroupDomainModel>
)

data class BookGroupDomainModel(
    val bookId: Int,
    val bookTitle: String,
    val quotes: List<QuoteGroupDomainModel>
)

data class QuoteGroupDomainModel(
    val id: Int,
    val selectedText: String,
    val positionStart: Int,
    val positionEnd: Int,
    val noteContent: String?
)