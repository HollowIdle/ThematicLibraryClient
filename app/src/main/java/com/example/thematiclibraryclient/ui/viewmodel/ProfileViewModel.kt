package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.example.thematiclibraryclient.domain.usecase.auth.LogoutUseCase
import com.example.thematiclibraryclient.domain.usecase.users.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserDomainModel? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)
            when (val result = getUserInfoUseCase()) {
                is TResult.Success -> {
                    _uiState.value = ProfileUiState(user = result.data)
                }
                is TResult.Error -> {
                    _uiState.value = ProfileUiState(error = "Не удалось загрузить данные профиля")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _eventFlow.emit(UiEvent.NavigateToAuth)
        }
    }

    sealed class UiEvent {
        object NavigateToAuth : UiEvent()
    }
}