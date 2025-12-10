package com.example.thematiclibraryclient.data.remote.model.bookmarks

import com.google.gson.annotations.SerializedName

data class CreateBookmarkRequestApiModel(
    @SerializedName("position")
    val position: Int,

    @SerializedName("note")
    val note: String?
)