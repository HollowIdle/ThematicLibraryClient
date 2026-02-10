package com.example.thematiclibraryclient.domain.model.books

data class BookDetailsDomainModel(
    val id: Int,
    val title: String,
    val description: String?,
    val authors: List<String>,
    val tags: List<String>,
    val shelfIds: List<Int>,
    val lastPosition: Int,
    val fileExtension: String? = null
)