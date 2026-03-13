package com.example.thematiclibraryclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import java.util.Date

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = -1,
    val serverId: Int? = null,
    val username: String,
    val email: String,
    val isBlocked: Boolean = false,
    val diskQuota: Long = 524288000,
    val lastSessionReset: Date? = null
)

fun UserEntity.toDomainModel() = UserDomainModel(
    username = username,
    email = email,
    isBlocked = isBlocked,
    diskQuota = diskQuota,
    lastSessionReset = lastSessionReset
)