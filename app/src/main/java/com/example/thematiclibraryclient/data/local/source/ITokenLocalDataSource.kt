package com.example.thematiclibraryclient.data.local.source

import kotlinx.coroutines.flow.Flow

interface ITokenLocalDataSource {

    fun getToken() : Flow<String?>

    suspend fun saveToken(token: String)

    suspend fun clearToken()

}