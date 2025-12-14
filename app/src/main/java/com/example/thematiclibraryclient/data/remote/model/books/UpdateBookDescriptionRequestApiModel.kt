package com.example.thematiclibraryclient.data.remote.model.books

import com.google.gson.annotations.SerializedName

data class UpdateDescriptionRequestApiModel(
    @SerializedName("description")
    val description: String
)