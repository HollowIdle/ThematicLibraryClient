package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel

interface IUserRemoteRepository {
    suspend fun getUserInfo(): TResult<UserDomainModel, ConnectionExceptionDomainModel>
}