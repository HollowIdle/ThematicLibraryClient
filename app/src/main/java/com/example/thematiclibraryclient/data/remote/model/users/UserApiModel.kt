package com.example.thematiclibraryclient.data.remote.model.users

import com.example.thematiclibraryclient.data.local.entity.UserEntity
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserApiModel(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("isBlocked")
    val isBlocked: Boolean = false,
    @SerializedName("storageQuota")
    val storageQuota: Long = 500,
    @SerializedName("lastSessionReset")
    val lastSessionReset: String? = null
) {
    companion object {
        private const val MB_TO_BYTES = 1048576L
    }

    fun toDomainModel(): UserDomainModel {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val parsedDate = lastSessionReset?.let {
            try {
                dateFormat.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        return UserDomainModel(
            username = username,
            email = email,
            isBlocked = isBlocked,
            diskQuota = storageQuota * MB_TO_BYTES,
            lastSessionReset = parsedDate
        )
    }

    fun toEntity(): UserEntity {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val parsedDate = lastSessionReset?.let {
            try {
                dateFormat.parse(it)
            } catch (e: Exception) {
                null
            }
        }

        return UserEntity(
            id = 0,
            username = this.username,
            email = this.email,
            isBlocked = this.isBlocked,
            diskQuota = this.storageQuota * MB_TO_BYTES,
            lastSessionReset = parsedDate
        )
    }
}
