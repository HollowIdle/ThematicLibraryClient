package com.example.thematiclibraryclient.domain.usecase.auth

import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import jakarta.inject.Inject

class SetOfflineModeUseCase @Inject constructor(
    private val tokenLocalDataSource: ITokenLocalDataSource
) {
    suspend operator fun invoke(isOffline: Boolean) {
        tokenLocalDataSource.setOfflineMode(isOffline)
    }
}