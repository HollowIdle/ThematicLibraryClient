package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import kotlinx.coroutines.flow.Flow

interface IUserRemoteRepository {
    fun getUser(): Flow<UserDomainModel?>

    suspend fun refreshUser(): TResult<Unit, ConnectionExceptionDomainModel>

    suspend fun clearUserData()
}