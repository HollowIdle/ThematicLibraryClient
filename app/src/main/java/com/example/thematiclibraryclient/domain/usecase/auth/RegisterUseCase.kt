package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.IAuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(username: String, email: String, password: String) =
        authRepository.register(username, email, password)
}