package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.UserDao
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IUserApi
import com.example.thematiclibraryclient.data.remote.model.users.toDomainModel
import com.example.thematiclibraryclient.data.remote.model.users.toEntity
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.example.thematiclibraryclient.domain.repository.IUserRemoteRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRemoteRepositoryImpl @Inject constructor(
    private val userApi: IUserApi,
    private val userDao: UserDao
) : IUserRemoteRepository {

    override fun getUser(): Flow<UserDomainModel?> {
        return userDao.getUser().map { it?.toDomainModel() }
    }

    override suspend fun refreshUser(): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiUser = userApi.getUserInfo()
            userDao.insertUser(apiUser.toEntity())
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun clearUserData() {
        userDao.clearUser()
    }
}