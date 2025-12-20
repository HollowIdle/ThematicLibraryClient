package com.example.thematiclibraryclient.domain.model.quotes

data class QuoteDomainModel(
    val id: Int,
    val selectedText: String,
    val positionStart: Int,
    val positionEnd: Int,
    val bookId: Int,
    val bookTitle: String = "",
    val noteContent: String? = null,
    val locatorData: String? = null
)