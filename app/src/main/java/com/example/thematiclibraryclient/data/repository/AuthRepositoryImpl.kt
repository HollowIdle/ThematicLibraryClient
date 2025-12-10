package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.mapper.toAuthExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IAuthApi
import com.example.thematiclibraryclient.data.remote.model.auth.LoginRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.auth.RegisterRequestApiModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.auth.AuthExceptionDomainModel
import com.example.thematiclibraryclient.domain.repository.IAuthRepository
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: IAuthApi
) : IAuthRepository {

    override suspend fun register(username: String, email: String, password: String) : TResult<Unit, AuthExceptionDomainModel> {
        return try {
            authApi.register(RegisterRequestApiModel(username, email, password))
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toAuthExceptionDomainModel())
        }
    }

    override suspend fun login(email: String, password: String) : TResult<String, AuthExceptionDomainModel> {
        return try {
            val response = authApi.login(LoginRequestApiModel(email, password))
            TResult.Success(response.token)
        } catch (e: Throwable) {
            TResult.Error(e.toAuthExceptionDomainModel())
        }
    }
}