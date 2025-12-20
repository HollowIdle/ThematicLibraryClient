package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.usecase.shelves.GetBooksOnShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.RefreshBooksOnShelfUseCase
import com.example.thematiclibraryclient.ui.common.BookFilterEngine
import com.example.thematiclibraryclient.ui.common.SearchScope
import com.example.thematiclibraryclient.ui.model.BookFilterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShelfBooksUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val shelfName: String = "",
    val allBooks: List<BookDomainModel> = emptyList(),
    val filteredBooks: List<BookDomainModel> = emptyList(),
    val searchQuery: String = "",
    val currentSearchScope: SearchScope = SearchScope.Everywhere,
    val filterState: BookFilterState = BookFilterState(),
    val availableAuthors: List<AuthorDomainModel?> = emptyList(),
    val availableTags: List<String> = emptyList()
)
@HiltViewModel
class ShelfBooksViewModel @Inject constructor(
    private val getBooksOnShelfUseCase: GetBooksOnShelfUseCase,
    private val filterEngine: BookFilterEngine,
    private val refreshBooksOnShelfUseCase: RefreshBooksOnShelfUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val shelfId: Int = checkNotNull(savedStateHandle["shelfId"])

    private val _uiState = MutableStateFlow(ShelfBooksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        subscribeToBooks()
        refreshBooks()
    }

    private fun subscribeToBooks() {
        viewModelScope.launch {
            getBooksOnShelfUseCase(shelfId).collect { books ->
                val filterOptions = filterEngine.getAvailableFilters(books)
                _uiState.value = _uiState.value.copy(
                    allBooks = books,
                    availableAuthors = filterOptions.authors,
                    availableTags = filterOptions.tags
                )
                applyFilters()
            }
        }
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = refreshBooksOnShelfUseCase(shelfId)) {
                is TResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is TResult.Error -> {
                    val errorMsg = if (_uiState.value.allBooks.isEmpty()) "Ошибка загрузки" else null
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onSearchScopeChanged(scope: SearchScope) {
        _uiState.value = _uiState.value.copy(currentSearchScope = scope)
        applyFilters()
    }

    fun onFilterChanged(newFilterState: BookFilterState) {
        _uiState.value = _uiState.value.copy(filterState = newFilterState)
        applyFilters()
    }

    fun clearFilters() {
        onFilterChanged(BookFilterState())
    }

    private fun applyFilters() {
        val state = _uiState.value

        val filtered = filterEngine.filter(
            books = state.allBooks,
            query = state.searchQuery,
            scope = state.currentSearchScope,
            filterState = state.filterState
        )

        _uiState.value = _uiState.value.copy(filteredBooks = filtered)
    }

}