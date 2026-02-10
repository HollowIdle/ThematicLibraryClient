package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import jakarta.inject.Inject

class GetOfflineModeUseCase @Inject constructor(
    private val tokenLocalDataSource: ITokenLocalDataSource
) {
    operator fun invoke() = tokenLocalDataSource.isOfflineMode()
}