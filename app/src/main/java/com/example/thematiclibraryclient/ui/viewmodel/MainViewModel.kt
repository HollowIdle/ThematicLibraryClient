package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.usecase.auth.ClearTokenUseCase
import com.example.thematiclibraryclient.domain.usecase.auth.GetAuthStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
}