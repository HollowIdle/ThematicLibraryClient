package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.auth.AuthExceptionDomainModel
import com.example.thematiclibraryclient.domain.usecase.auth.LoginUseCase
import com.example.thematiclibraryclient.domain.usecase.auth.RegisterUseCase
import com.example.thematiclibraryclient.domain.usecase.auth.SaveTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null
)

data class RegisterUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val saveTokenUseCase: SaveTokenUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState = _registerUiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (!validateEmail(trimmedEmail) || !validateLoginPassword(trimmedPassword)) return

        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)
            when(val result = loginUseCase(trimmedEmail, trimmedPassword)){
                is TResult.Success -> {
                    saveTokenUseCase(result.data)
                    _loginUiState.value = LoginUiState(isLoading = false)
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is AuthExceptionDomainModel.InvalidCredentials -> "Неверный логин или пароль"
                        is AuthExceptionDomainModel.NoInternet -> "Ошибка сети"
                        else -> "Неизвестная ошибка"
                    }
                    _loginUiState.value = LoginUiState(isLoading = false, error = errorMessage)
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        val trimmedUsername = username.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (!validateRegisterEmail(trimmedEmail) || !validateRegisterPassword(trimmedPassword) || !validateUsername(trimmedUsername)) return

        viewModelScope.launch {
            _registerUiState.value = RegisterUiState(isLoading = true)
            when (val result = registerUseCase(trimmedUsername, trimmedEmail, trimmedPassword)) {
                is TResult.Success -> {
                    _registerUiState.value = RegisterUiState(isLoading = false, success = true)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Регистрация прошла успешно"))
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is AuthExceptionDomainModel.UserAlreadyExists -> "Пользователь с таким email уже существует."
                        is AuthExceptionDomainModel.NoInternet -> "Ошибка сети. Проверьте подключение."
                        else -> "Произошла ошибка при регистрации."
                    }
                    _registerUiState.value = RegisterUiState(isLoading = false, error = errorMessage)
                }
            }
        }
    }

    // --- Валидация ---
    private fun validateEmail(email: String): Boolean {
        if (email.isBlank()) {
            _loginUiState.value = _loginUiState.value.copy(emailError = "Email не может быть пустым")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginUiState.value = _loginUiState.value.copy(emailError = "Неверный формат Email")
            return false
        }
        _loginUiState.value = _loginUiState.value.copy(emailError = null)
        return true
    }

    private fun validateRegisterEmail(email: String): Boolean {
        if (email.isBlank()) {
            _registerUiState.value = _registerUiState.value.copy(emailError = "Email не может быть пустым")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerUiState.value = _registerUiState.value.copy(emailError = "Неверный формат Email")
            return false
        }
        _registerUiState.value = _registerUiState.value.copy(emailError = null)
        return true
    }

    private fun validateUsername(username: String): Boolean {
        if (username.isBlank()) {
            _registerUiState.value = _registerUiState.value.copy(usernameError = "Имя пользователя не может быть пустым")
            return false
        }
        if (username.length < 4) {
            _registerUiState.value = _registerUiState.value.copy(usernameError = "Имя пользователя должно быть длиннее 3 символов")
            return false
        }
        val validUsernamePattern = Regex("^[a-zA-Z0-9_]+$")
        if (!validUsernamePattern.matches(username)) {
            _registerUiState.value = _registerUiState.value.copy(usernameError = "Имя пользователя может содержать только латинские буквы, цифры и '_'")
            return false
        }
        _registerUiState.value = _registerUiState.value.copy(usernameError = null)
        return true
    }

    private fun validateLoginPassword(password: String): Boolean {
        if (password.isBlank()) {
            _loginUiState.value = _loginUiState.value.copy(passwordError = "Пароль не может быть пустым")
            return false
        }
        _loginUiState.value = _loginUiState.value.copy(passwordError = null)
        return true
    }

    private fun validateRegisterPassword(password: String): Boolean {
        if (password.isBlank()) {
            _registerUiState.value = _registerUiState.value.copy(passwordError = "Пароль не может быть пустым")
            return false
        }
        if (password.length < 6) {
            _registerUiState.value = _registerUiState.value.copy(passwordError = "Пароль должен быть не менее 6 символов")
            return false
        }
        _registerUiState.value = _registerUiState.value.copy(passwordError = null)
        return true
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
