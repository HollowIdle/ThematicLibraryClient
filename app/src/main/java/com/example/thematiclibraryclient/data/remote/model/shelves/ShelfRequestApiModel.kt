package com.example.thematiclibraryclient.data.remote.model.shelves

import com.google.gson.annotations.SerializedName

data class ShelfRequestApiModel(
    @SerializedName("name")
    val name: String
)