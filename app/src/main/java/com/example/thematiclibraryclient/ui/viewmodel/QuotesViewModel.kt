package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.example.thematiclibraryclient.domain.usecase.notes.UpsertNoteUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.DeleteQuoteUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.GetFlatQuotesUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.GetGroupedQuotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuotesViewMode {
    GROUPED, FLAT
}
data class QuotesUiState(
    val isLoading: Boolean = false,
    val groupedQuotes: List<ShelfGroupDomainModel> = emptyList(),
    val error: String? = null,
    val currentViewMode: QuotesViewMode = QuotesViewMode.GROUPED,
    val flatQuotes: List<QuoteDomainModel> = emptyList(),
    val selectedQuote: QuoteDomainModel? = null
)

@HiltViewModel
class QuotesViewModel @Inject constructor(
    private val getGroupedQuotesUseCase: GetGroupedQuotesUseCase,
    private val getFlatQuotesUseCase: GetFlatQuotesUseCase,
    private val deleteQuoteUseCase: DeleteQuoteUseCase,
    private val upsertNoteUseCase: UpsertNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuotesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadQuotes()
    }

    fun setViewMode(mode: QuotesViewMode) {
        _uiState.value = _uiState.value.copy(currentViewMode = mode)
        loadQuotes()
    }

    fun loadQuotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (_uiState.value.currentViewMode) {
                QuotesViewMode.GROUPED -> loadGroupedQuotes()
                QuotesViewMode.FLAT -> loadFlatQuotes()
            }
        }
    }

    private suspend fun loadGroupedQuotes() {
        when (val result = getGroupedQuotesUseCase()) {
            is TResult.Success -> {
                _uiState.value = _uiState.value.copy(isLoading = false, groupedQuotes = result.data)
            }
            is TResult.Error -> {
                val errorMessage = when (result.exception) {
                    is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети."
                    else -> "Произошла неизвестная ошибка."
                }
                _uiState.value = QuotesUiState(error = errorMessage)
            }
        }
    }

    private suspend fun loadFlatQuotes() {
        when (val result = getFlatQuotesUseCase()) {
            is TResult.Success -> {
                _uiState.value = _uiState.value.copy(isLoading = false, flatQuotes = result.data)
            }
            is TResult.Error -> {
                val errorMessage = when (result.exception) {
                    is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети."
                    else -> "Произошла неизвестная ошибка."
                }
                _uiState.value = QuotesUiState(error = errorMessage)
            }
        }
    }

    fun deleteQuote(quoteToDelete: QuoteDomainModel) {
        viewModelScope.launch {
            when (val result = deleteQuoteUseCase(quoteToDelete.id)) {
                is TResult.Success -> {
                    val updatedList = _uiState.value.flatQuotes.filterNot { it.id == quoteToDelete.id }
                    _uiState.value = _uiState.value.copy(flatQuotes = updatedList, selectedQuote = null)
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети."
                        else -> "Произошла неизвестная ошибка."
                    }
                    _uiState.value = QuotesUiState(error = errorMessage)
                }
            }
        }
    }

    fun saveNoteForSelectedQuote(noteContent: String) {
        val quoteToUpdate = _uiState.value.selectedQuote ?: return
        viewModelScope.launch {
            when (val result = upsertNoteUseCase(quoteToUpdate.id, noteContent)) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(selectedQuote = null)
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети."
                        else -> "Произошла неизвестная ошибка."
                    }
                    _uiState.value = QuotesUiState(error = errorMessage)
                }
            }
        }
    }

    fun onQuoteSelected(quote: QuoteDomainModel) {
        _uiState.value = _uiState.value.copy(selectedQuote = quote)
    }

    fun onDialogDismiss() {
        _uiState.value = _uiState.value.copy(selectedQuote = null)
    }
}