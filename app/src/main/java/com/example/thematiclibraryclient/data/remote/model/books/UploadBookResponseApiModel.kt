package com.example.thematiclibraryclient.data.remote.model.books

import com.google.gson.annotations.SerializedName

data class UploadBookResponseApiModel(
    @SerializedName("message")
    val message: String,

    @SerializedName("bookId")
    val bookId: Int,

    @SerializedName("title")
    val title: String
)