package com.example.thematiclibraryclient.data.remote.model.notes

import com.example.thematiclibraryclient.domain.model.quotes.NoteDomainModel
import com.google.gson.annotations.SerializedName

data class NoteApiModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("content")
    val content: String,

    @SerializedName("quoteId")
    val quoteId: Int
)


fun NoteApiModel.toDomainModel() =
    NoteDomainModel(
        content = content,
        quoteId = quoteId
    )