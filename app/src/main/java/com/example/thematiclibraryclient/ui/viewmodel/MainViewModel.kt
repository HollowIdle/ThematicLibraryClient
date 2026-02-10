package com.example.thematiclibraryclient.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.thematiclibraryclient.data.worker.SyncWorker
import com.example.thematiclibraryclient.domain.usecase.auth.ClearTokenUseCase
import com.example.thematiclibraryclient.domain.usecase.auth.GetAuthStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed class AuthState {
    object Loading: AuthState()
    object Authenticated: AuthState()
    object Unauthenticated: AuthState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getAuthStatusUseCase: GetAuthStatusUseCase,
    private val clearTokenUseCase: ClearTokenUseCase
) : ViewModel() {

    val authState: StateFlow<AuthState> = getAuthStatusUseCase()
        .map {
            isAuthenticated ->
            if(isAuthenticated) {
                AuthState.Authenticated
            }
            else {
                AuthState.Unauthenticated
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    fun onSessionExpired(){
        viewModelScope.launch {
            clearTokenUseCase()
        }
    }

    fun startSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ManualSync",
            ExistingWorkPolicy.KEEP,
            oneTimeRequest
        )
    }
}