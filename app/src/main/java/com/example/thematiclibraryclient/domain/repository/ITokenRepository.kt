package com.example.thematiclibraryclient.domain.repository

import kotlinx.coroutines.flow.Flow

interface ITokenRepository {
    suspend fun saveToken(token: String)
    fun getToken(): Flow<String?>

    suspend fun clearToken()
}