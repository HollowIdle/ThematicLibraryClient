package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.users.UserApiModel
import retrofit2.http.GET

interface IUserApi {
    @GET("/api/users/me")
    suspend fun getUserInfo(): UserApiModel
}