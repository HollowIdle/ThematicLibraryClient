package com.example.thematiclibraryclient.data.remote.model.auth

import com.google.gson.annotations.SerializedName

data class LoginResponseApiModel(
    @SerializedName("token")
    val token: String
)
