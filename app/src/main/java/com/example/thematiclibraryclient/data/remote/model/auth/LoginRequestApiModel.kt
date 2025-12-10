package com.example.thematiclibraryclient.data.remote.model.auth

import com.google.gson.annotations.SerializedName

data class LoginRequestApiModel(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)