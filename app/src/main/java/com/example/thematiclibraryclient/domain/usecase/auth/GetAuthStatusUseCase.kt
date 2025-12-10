package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAuthStatusUseCase @Inject constructor(
    private val tokenRepository: ITokenRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return tokenRepository.getToken().map { token ->
            !token.isNullOrBlank()
        }
    }
}