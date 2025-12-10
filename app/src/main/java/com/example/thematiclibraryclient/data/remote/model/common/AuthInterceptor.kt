package com.example.thematiclibraryclient.data.remote.model.common

import android.util.Log
import com.example.thematiclibraryclient.data.common.SessionExpiredNotifier
import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenLocalDataSource: ITokenLocalDataSource,
    private val notifier: SessionExpiredNotifier
) : Interceptor {

    @OptIn(DelicateCoroutinesApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val originalRequest = chain.request()

        val token = runBlocking {
            tokenLocalDataSource.getToken().firstOrNull()
        }

        Log.d("AuthInterceptor", "Request to ${originalRequest.url.encodedPath}. Token used: '$token'")

        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        if(token.isNullOrEmpty() || originalRequest.url.encodedPath.contains("/auth/")){
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(newRequest)

        if(response.code == 401){
            GlobalScope.launch {
                notifier.notify()
            }
        }

        return response
    }

}