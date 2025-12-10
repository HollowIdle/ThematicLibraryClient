package com.example.thematiclibraryclient.data.remote.model.auth

import com.google.gson.annotations.SerializedName

data class RegisterResponseApiModel(
    @SerializedName("message")
    val message: String
)
