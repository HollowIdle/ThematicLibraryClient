package com.example.thematiclibraryclient.data.remote.model.books

import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.google.gson.annotations.SerializedName

data class AuthorApiModel(
    @SerializedName("name")
    val name: String
)

fun AuthorApiModel.toDomainModel() =
    AuthorDomainModel(
        name = name
    )