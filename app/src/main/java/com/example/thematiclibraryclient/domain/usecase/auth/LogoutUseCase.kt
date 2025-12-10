package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import jakarta.inject.Inject

class LogoutUseCase @Inject constructor(
    private val tokenRepository: ITokenRepository
) {
    suspend operator fun invoke() {
        tokenRepository.saveToken("")
    }
}