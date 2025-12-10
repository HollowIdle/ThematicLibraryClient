package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.domain.repository.IAuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.login(email,password)
}