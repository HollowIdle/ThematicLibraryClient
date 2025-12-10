package com.example.thematiclibraryclient.data.remote.model.books

import com.google.gson.annotations.SerializedName

data class BookProgressRequestApiModel(
    @SerializedName("lastPosition")
    val lastPosition: Int
)