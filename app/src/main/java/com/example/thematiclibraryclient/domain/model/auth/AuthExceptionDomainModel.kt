package com.example.thematiclibraryclient.domain.model.auth

sealed class AuthExceptionDomainModel(exception: Throwable) : Throwable(exception) {
    override val cause: Throwable = exception

    class UserAlreadyExists(exception: Throwable) : AuthExceptionDomainModel(exception)
    class InvalidCredentials(exception: Throwable) : AuthExceptionDomainModel(exception)
    class NoInternet(exception: Throwable) : AuthExceptionDomainModel(exception)
    class Other(exception: Throwable) : AuthExceptionDomainModel(exception)
}