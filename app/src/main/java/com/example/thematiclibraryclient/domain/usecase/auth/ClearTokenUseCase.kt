package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import javax.inject.Inject

class ClearTokenUseCase @Inject constructor(
    private val tokenRepository: ITokenRepository
) {
    suspend operator fun invoke() = tokenRepository.clearToken()
}