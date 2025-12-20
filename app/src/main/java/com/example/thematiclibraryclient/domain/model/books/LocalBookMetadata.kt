package com.example.thematiclibraryclient.domain.model.books

data class LocalBookMetadata(
    val title: String,
    val authors: List<String>,
    val description: String? = null
)