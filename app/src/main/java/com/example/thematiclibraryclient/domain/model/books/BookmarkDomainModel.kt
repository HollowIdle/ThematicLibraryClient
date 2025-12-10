package com.example.thematiclibraryclient.domain.model.books

data class BookmarkDomainModel(
    val id: Int,
    val position: Int,
    val note: String?
)