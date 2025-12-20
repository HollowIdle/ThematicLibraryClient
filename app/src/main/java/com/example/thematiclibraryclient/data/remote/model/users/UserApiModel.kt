package com.example.thematiclibraryclient.data.remote.model.users

import com.example.thematiclibraryclient.data.local.entity.UserEntity
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

fun UserApiModel.toEntity() = UserEntity(
    id = 0,
    username = this.username,
    email = this.email
)
