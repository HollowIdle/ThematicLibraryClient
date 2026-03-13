package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TokenRepositoryImpl @Inject constructor(
    private val localDataSource: ITokenLocalDataSource
) : ITokenRepository {

    override suspend fun saveToken(token: String) {
        localDataSource.saveToken(token)
    }

    override fun getToken(): Flow<String?> {
        return localDataSource.getToken()
    }

    override suspend fun getTokenSync(): String? {
        return localDataSource.getToken().first()
    }

    override suspend fun clearToken() {
        return localDataSource.clearToken()
    }

}