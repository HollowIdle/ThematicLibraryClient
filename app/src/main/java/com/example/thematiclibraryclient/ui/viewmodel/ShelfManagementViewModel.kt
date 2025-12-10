package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.usecase.shelves.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShelfManagementUiState(
    val isLoading: Boolean = false,
    val shelves: List<ShelfDomainModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ShelfManagementViewModel @Inject constructor(
    private val getShelvesUseCase: GetShelvesUseCase,
    private val createShelfUseCase: CreateShelfUseCase,
    private val updateShelfUseCase: UpdateShelfUseCase,
    private val deleteShelfUseCase: DeleteShelfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShelfManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadShelves()
    }

    fun loadShelves() {
        viewModelScope.launch {
            _uiState.value = ShelfManagementUiState(isLoading = true)
            when (val result = getShelvesUseCase()) {
                is TResult.Success -> _uiState.value = ShelfManagementUiState(shelves = result.data)
                is TResult.Error -> _uiState.value = ShelfManagementUiState(error = "Ошибка загрузки полок")
            }
        }
    }

    fun createShelf(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                when (val result = createShelfUseCase(name)) {
                    is TResult.Success -> {
                        val updatedList = _uiState.value.shelves + result.data
                        _uiState.value = _uiState.value.copy(shelves = updatedList)
                    }
                    is TResult.Error -> {
                        _uiState.value = ShelfManagementUiState(error = "Ошибка создания полки")
                    }
                }
            }
        }
    }

    fun updateShelf(shelfId: Int, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                when (updateShelfUseCase(shelfId, newName)) {
                    is TResult.Success -> {
                        val updatedList = _uiState.value.shelves.map {
                            if (it.id == shelfId) it.copy(name = newName) else it
                        }
                        _uiState.value = _uiState.value.copy(shelves = updatedList)
                    }
                    is TResult.Error -> {
                        _uiState.value = ShelfManagementUiState(error = "Ошибка обновления полки")
                    }
                }
            }
        }
    }

    fun deleteShelf(shelfId: Int) {
        viewModelScope.launch {
            when (deleteShelfUseCase(shelfId)) {
                is TResult.Success -> {
                    val updatedList = _uiState.value.shelves.filterNot { it.id == shelfId }
                    _uiState.value = _uiState.value.copy(shelves = updatedList)
                }
                is TResult.Error -> {
                    _uiState.value = ShelfManagementUiState(error = "Ошибка удаления полки")
                }
            }
        }
    }
}