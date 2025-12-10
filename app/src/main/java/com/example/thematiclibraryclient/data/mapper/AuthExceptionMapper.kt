package com.example.thematiclibraryclient.data.mapper

import com.example.thematiclibraryclient.domain.model.auth.AuthExceptionDomainModel
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException

fun Throwable.toAuthExceptionDomainModel(): AuthExceptionDomainModel {
    return when (this) {
        is UnknownHostException, is ConnectException ->
            AuthExceptionDomainModel.NoInternet(this)
        is HttpException ->
            when (this.code()) {
                409 -> AuthExceptionDomainModel.UserAlreadyExists(this)
                400, 401 -> AuthExceptionDomainModel.InvalidCredentials(this)
                else -> AuthExceptionDomainModel.Other(this)
            }
        is AuthExceptionDomainModel -> this
        else -> AuthExceptionDomainModel.Other(this)
    }
}