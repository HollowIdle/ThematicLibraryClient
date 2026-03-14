package com.example.thematiclibraryclient.domain.model.user

import java.util.Date

data class UserDomainModel(
    val username: String,
    val email: String,
    val isBlocked: Boolean = false,
    val diskQuota: Long = 524288000,
    val storageUsed: Long = 0,
    val lastSessionReset: Date? = null
)