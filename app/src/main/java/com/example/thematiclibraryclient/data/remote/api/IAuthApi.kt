package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.auth.AuthResponseApiModel
import com.example.thematiclibraryclient.data.remote.model.auth.LoginRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.auth.RegisterRequestApiModel
import retrofit2.http.Body
import retrofit2.http.POST

interface IAuthApi {

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequestApiModel) : AuthResponseApiModel

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestApiModel) : AuthResponseApiModel

}