package com.example.thematiclibraryclient.data.remote.model.notes

import com.google.gson.annotations.SerializedName

data class NoteRequestApiModel(
    @SerializedName("content")
    val content: String
)