package com.example.thematiclibraryclient.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.thematiclibraryclient.data.worker.SyncWorker
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.user.UserDomainModel
import com.example.thematiclibraryclient.domain.usecase.auth.GetOfflineModeUseCase
import com.example.thematiclibraryclient.domain.usecase.auth.LogoutUseCase
import com.example.thematiclibraryclient.domain.usecase.users.GetUserInfoUseCase
import com.example.thematiclibraryclient.domain.usecase.users.RefreshUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val user: UserDomainModel? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val refreshUserUseCase: RefreshUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getOfflineModeUseCase: GetOfflineModeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        subscribeToData()
        refreshUser()
    }

    private fun subscribeToData() {
        viewModelScope.launch {
            combine(
                getUserInfoUseCase(),
                getOfflineModeUseCase()
            ) { dbUser, isOffline ->
                Triple(dbUser, isOffline, isOffline)
            }.collect { (dbUser, isOffline, _) ->
                var newState = _uiState.value.copy(isOffline = isOffline)

                if (dbUser != null) {
                    newState = newState.copy(
                        user = dbUser,
                        isLoading = false,
                        error = null
                    )
                } else if (isOffline) {
                    newState = newState.copy(
                        user = UserDomainModel(
                            username = "(Офлайн)",
                            email = "Синхронизация недоступна"
                        ),
                        isLoading = false,
                        error = null
                    )
                }
                _uiState.value = newState
            }
        }
    }

    fun loadUser() {
        refreshUser()
    }

    private fun refreshUser() {
        viewModelScope.launch {
            if (getOfflineModeUseCase().first()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = refreshUserUseCase()) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is TResult.Error -> {
                    val currentState = _uiState.value
                    if (currentState.user == null) {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = "Не удалось загрузить профиль"
                        )
                    } else {
                        _uiState.value = currentState.copy(isLoading = false)
                    }
                }
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            if (getOfflineModeUseCase().first()) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Невозможно синхронизировать в офлайн-режиме"))
                return@launch
            }

            refreshUserUseCase()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            val workManager = WorkManager.getInstance(context)

            workManager.enqueueUniqueWork(
                "ManualSyncProfile",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            workManager.getWorkInfoByIdFlow(syncRequest.id).collect { workInfo ->
                if (workInfo == null) return@collect

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                        _uiState.value = _uiState.value.copy(isSyncing = true)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.value = _uiState.value.copy(isSyncing = false)
                        _eventFlow.emit(UiEvent.ShowSnackbar("Синхронизация успешно завершена"))
                    }
                    WorkInfo.State.FAILED -> {
                        _uiState.value = _uiState.value.copy(isSyncing = false)
                        _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка синхронизации"))
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.value = _uiState.value.copy(isSyncing = false)
                    }
                    else -> {}
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
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}