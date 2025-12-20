package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import com.example.thematiclibraryclient.domain.repository.IUserRemoteRepository
import jakarta.inject.Inject

class LogoutUseCase @Inject constructor(
    private val tokenRepository: ITokenRepository,
    private val userRepository: IUserRemoteRepository
) {
    suspend operator fun invoke() {
        tokenRepository.saveToken("")
        userRepository.clearUserData()
    }
}