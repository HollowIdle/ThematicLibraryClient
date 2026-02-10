package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = -1,
    val serverId: Int? = null,
    val username: String,
    val email: String
)

fun UserEntity.toDomainModel() = UserDomainModel(
    username = username,
    email = email
)