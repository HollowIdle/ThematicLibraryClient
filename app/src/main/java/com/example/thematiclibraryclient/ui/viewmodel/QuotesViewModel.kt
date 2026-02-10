package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.data.common.NetworkConnectivityObserver
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.example.thematiclibraryclient.domain.usecase.notes.UpsertNoteUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.DeleteQuoteUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.GetFlatQuotesUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.GetGroupedQuotesUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.RefreshQuotesUseCase
import com.example.thematiclibraryclient.ui.common.SearchScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val selectedQuote: QuoteDomainModel? = null,
    val searchQuery: String = "",
    val currentSearchScope: SearchScope = SearchScope.Everywhere,
    val allGroupedQuotes: List<ShelfGroupDomainModel> = emptyList(),
    val allFlatQuotes: List<QuoteDomainModel> = emptyList(),
    val filteredGroupedQuotes: List<ShelfGroupDomainModel> = emptyList(),
    val filteredFlatQuotes: List<QuoteDomainModel> = emptyList()
)

@HiltViewModel
class QuotesViewModel @Inject constructor(
    private val getGroupedQuotesUseCase: GetGroupedQuotesUseCase,
    private val getFlatQuotesUseCase: GetFlatQuotesUseCase,
    private val deleteQuoteUseCase: DeleteQuoteUseCase,
    private val refreshQuotesUseCase: RefreshQuotesUseCase,
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val networkObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuotesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        subscribeToQuotes()
        refreshQuotes()
    }

    private fun subscribeToQuotes() {
        viewModelScope.launch {
            launch {
                getFlatQuotesUseCase().collect { quotes ->
                    _uiState.value = _uiState.value.copy(allFlatQuotes = quotes)
                    applyFilters()
                }
            }
            launch {
                getGroupedQuotesUseCase().collect { grouped ->
                    _uiState.value = _uiState.value.copy(allGroupedQuotes = grouped)
                    applyFilters()
                }
            }
        }
    }

    fun refreshQuotes() {
        viewModelScope.launch {
            val hasData = _uiState.value.allFlatQuotes.isNotEmpty()

            val isOnline = networkObserver.observe().first()
            if (!isOnline) {
                if (!hasData) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                }
                return@launch
            }

            if (!hasData) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            when (val result = refreshQuotesUseCase()) {
                is TResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is TResult.Error -> {
                    if (!hasData) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    fun setViewMode(mode: QuotesViewMode) { _uiState.value = _uiState.value.copy(currentViewMode = mode) }
    fun onSearchQueryChanged(query: String) { _uiState.value = _uiState.value.copy(searchQuery = query); applyFilters() }
    fun onSearchScopeChanged(scope: SearchScope) { _uiState.value = _uiState.value.copy(currentSearchScope = scope); applyFilters() }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        val scope = state.currentSearchScope

        if (query.isBlank()) {
            _uiState.value = state.copy(filteredGroupedQuotes = state.allGroupedQuotes, filteredFlatQuotes = state.allFlatQuotes)
            return
        }

        val filteredFlat = state.allFlatQuotes.filter { matchQuote(it, query, scope) }
        val filteredGrouped = state.allGroupedQuotes.mapNotNull { shelf ->
            val filteredBooks = shelf.books.mapNotNull { book ->
                val isBookTitleMatch = (scope == SearchScope.Everywhere || scope == SearchScope.Title) && book.bookTitle.contains(query, ignoreCase = true)
                if (isBookTitleMatch) book else {
                    val matchingQuotes = book.quotes.filter { quoteGroup ->
                        (scope == SearchScope.Everywhere || scope == SearchScope.Quote) && (quoteGroup.selectedText.contains(query, ignoreCase = true) || (quoteGroup.noteContent?.contains(query, ignoreCase = true) == true))
                    }
                    if (matchingQuotes.isNotEmpty()) book.copy(quotes = matchingQuotes) else null
                }
            }
            if (filteredBooks.isNotEmpty()) shelf.copy(books = filteredBooks) else null
        }
        _uiState.value = state.copy(filteredFlatQuotes = filteredFlat, filteredGroupedQuotes = filteredGrouped)
    }

    private fun matchQuote(quote: QuoteDomainModel, query: String, scope: SearchScope): Boolean {
        val matchText = (scope == SearchScope.Everywhere || scope == SearchScope.Quote) && (quote.selectedText.contains(query, ignoreCase = true) || quote.noteContent?.contains(query, ignoreCase = true) == true)
        val matchTitle = (scope == SearchScope.Everywhere || scope == SearchScope.Title) && quote.bookTitle.contains(query, ignoreCase = true)
        return matchText || matchTitle
    }

    fun deleteQuote(quoteToDelete: QuoteDomainModel) {
        viewModelScope.launch {
            when (val result = deleteQuoteUseCase(quoteToDelete.id)) {
                is TResult.Success -> _uiState.value = _uiState.value.copy(selectedQuote = null)
                is TResult.Error -> _uiState.value = QuotesUiState(error = "Ошибка удаления")
            }
        }
    }

    fun saveNoteForSelectedQuote(noteContent: String) {
        val quoteToUpdate = _uiState.value.selectedQuote ?: return
        viewModelScope.launch {
            when (upsertNoteUseCase(quoteToUpdate.id, noteContent)) {
                is TResult.Success -> _uiState.value = _uiState.value.copy(selectedQuote = null)
                is TResult.Error -> _uiState.value = _uiState.value.copy(error = "Ошибка сохранения")
            }
        }
    }

    fun onQuoteSelected(quote: QuoteDomainModel) { _uiState.value = _uiState.value.copy(selectedQuote = quote) }
    fun onDialogDismiss() { _uiState.value = _uiState.value.copy(selectedQuote = null) }
}