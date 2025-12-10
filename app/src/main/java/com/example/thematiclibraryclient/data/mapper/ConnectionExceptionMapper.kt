package com.example.thematiclibraryclient.data.mapper

import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException

fun Throwable.toConnectionExceptionDomainModel(): ConnectionExceptionDomainModel {
    return when (this) {
        is UnknownHostException, is ConnectException ->
            ConnectionExceptionDomainModel.NoInternet(this)
        is HttpException ->
            when (this.code()) {
                401 -> ConnectionExceptionDomainModel.Unauthorized(this)
                else -> ConnectionExceptionDomainModel.Other(this)
            }
        is ConnectionExceptionDomainModel -> this
        else -> ConnectionExceptionDomainModel.Other(this)
    }
}