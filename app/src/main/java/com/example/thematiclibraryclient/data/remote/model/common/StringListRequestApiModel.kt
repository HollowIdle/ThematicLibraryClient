package com.example.thematiclibraryclient.data.remote.model.common

import com.google.gson.annotations.SerializedName

data class StringListRequestApiModel(
    @SerializedName("items")
    val items: List<String>
)