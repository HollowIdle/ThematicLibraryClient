package com.example.thematiclibraryclient.domain.model.common

sealed class ConnectionExceptionDomainModel(exception: Throwable) : Throwable(exception) {
    class Other(exception: Throwable) : ConnectionExceptionDomainModel(exception)
    class NoInternet(exception: Throwable) : ConnectionExceptionDomainModel(exception)
    class Unauthorized(exception: Throwable) : ConnectionExceptionDomainModel(exception)
}