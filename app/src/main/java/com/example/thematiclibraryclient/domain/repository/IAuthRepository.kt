package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.auth.AuthExceptionDomainModel

interface IAuthRepository {
    suspend fun register(username: String, email: String, password: String) : TResult<Unit, AuthExceptionDomainModel>
    suspend fun login(email: String, password: String) : TResult<String, AuthExceptionDomainModel>
}