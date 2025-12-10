package com.example.thematiclibraryclient.data.remote.model.auth

import com.google.gson.annotations.SerializedName

data class RegisterRequestApiModel(
    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)