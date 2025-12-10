package com.example.thematiclibraryclient.domain.usecase.users

import com.example.thematiclibraryclient.domain.repository.IUserRemoteRepository
import jakarta.inject.Inject

class GetUserInfoUseCase @Inject constructor(
    private val repository: IUserRemoteRepository
) {
    suspend operator fun invoke() = repository.getUserInfo()
}