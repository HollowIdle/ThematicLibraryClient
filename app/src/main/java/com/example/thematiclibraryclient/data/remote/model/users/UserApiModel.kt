package com.example.thematiclibraryclient.data.remote.model.users

import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.google.gson.annotations.SerializedName

data class UserApiModel(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String
)

fun UserApiModel.toDomainModel() = UserDomainModel(
    username = username,
    email = email
)
