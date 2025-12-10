package com.example.thematiclibraryclient.data.remote.model.quotes

import com.google.gson.annotations.SerializedName

data class CreateQuoteRequestApiModel(
    @SerializedName("selectedText")
    val selectedText: String,

    @SerializedName("positionStart")
    val positionStart: Int?,

    @SerializedName("positionEnd")
    val positionEnd: Int?,

    @SerializedName("note")
    val note: String?
)