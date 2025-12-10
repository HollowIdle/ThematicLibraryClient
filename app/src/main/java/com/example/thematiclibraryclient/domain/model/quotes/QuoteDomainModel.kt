package com.example.thematiclibraryclient.domain.model.quotes

data class QuoteDomainModel(
    val id: Int,
    val selectedText: String,
    val positionStart: Int,
    val positionEnd: Int
)