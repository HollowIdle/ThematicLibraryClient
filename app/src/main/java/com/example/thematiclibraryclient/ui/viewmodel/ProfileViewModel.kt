package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.example.thematiclibraryclient.domain.usecase.auth.LogoutUseCase
import com.example.thematiclibraryclient.domain.usecase.users.GetUserInfoUseCase
import com.example.thematiclibraryclient.domain.usecase.users.RefreshUserUseCase
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
    private val refreshUserUseCase: RefreshUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        subscribeToUser()
        refreshUser()
    }

    private fun subscribeToUser() {
        viewModelScope.launch {
            getUserInfoUseCase().collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(user = user)
                }
            }
        }
    }

    fun loadUser() {
        refreshUser()
    }

    private fun refreshUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = refreshUserUseCase()) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is TResult.Error -> {
                    val errorMsg = if (_uiState.value.user == null) "Не удалось загрузить профиль" else "Работаем оффлайн"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
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