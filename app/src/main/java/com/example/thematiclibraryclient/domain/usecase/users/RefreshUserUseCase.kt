package com.example.thematiclibraryclient.domain.usecase.users

import com.example.thematiclibraryclient.domain.repository.IUserRemoteRepository
import javax.inject.Inject

class RefreshUserUseCase @Inject constructor (
    private val repository: IUserRemoteRepository
) {
    suspend operator fun invoke() = repository.refreshUser()
}