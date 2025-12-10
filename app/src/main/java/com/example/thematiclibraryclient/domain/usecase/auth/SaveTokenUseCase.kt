package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import javax.inject.Inject

class SaveTokenUseCase @Inject constructor(
    private val tokenRepository: ITokenRepository
) {
    suspend operator fun invoke(token: String){
        tokenRepository.saveToken(token)
    }
}