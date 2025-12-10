package com.example.thematiclibraryclient.domain.model.books

data class BookDomainModel(
    val id: Int,
    val title: String,
    val description: String?,
    val authors: List<AuthorDomainModel?>
)