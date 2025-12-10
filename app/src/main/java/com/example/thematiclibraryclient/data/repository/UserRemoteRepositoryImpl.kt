package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IUserApi
import com.example.thematiclibraryclient.data.remote.model.users.toDomainModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.example.thematiclibraryclient.domain.repository.IUserRemoteRepository
import jakarta.inject.Inject

class UserRemoteRepositoryImpl @Inject constructor(
    private val userApi: IUserApi
) : IUserRemoteRepository {
    override suspend fun getUserInfo(): TResult<UserDomainModel, ConnectionExceptionDomainModel> {
        return try {
            TResult.Success(userApi.getUserInfo().toDomainModel())
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}