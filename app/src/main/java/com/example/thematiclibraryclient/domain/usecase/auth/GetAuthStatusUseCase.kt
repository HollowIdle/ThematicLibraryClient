package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAuthStatusUseCase @Inject constructor(
    private val tokenRepository: ITokenRepository,
    private val tokenLocalDataSource: ITokenLocalDataSource
) {
    operator fun invoke(): Flow<Boolean> {
        return tokenLocalDataSource.getToken().map { !it.isNullOrBlank() }
    }
}