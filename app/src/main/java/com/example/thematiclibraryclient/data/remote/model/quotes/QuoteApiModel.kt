package com.example.thematiclibraryclient.data.remote.model.quotes

import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.google.gson.annotations.SerializedName

data class QuoteApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("selectedText")
    val selectedText: String,

    @SerializedName("positionStart")
    val positionStart: Int,

    @SerializedName("positionEnd")
    val positionEnd: Int
)

fun QuoteApiModel.toDomainModel() = QuoteDomainModel(
    id = this.id,
    selectedText = this.selectedText,
    positionStart = this.positionStart,
    positionEnd = this.positionEnd
)