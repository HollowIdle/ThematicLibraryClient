package com.example.thematiclibraryclient.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.usecase.books.GetBooksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.RefreshBooksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UploadBookUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.CreateShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.DeleteShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.GetShelvesUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.RefreshShelvesUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.UpdateShelfUseCase
import com.example.thematiclibraryclient.ui.common.BookFilterEngine
import com.example.thematiclibraryclient.ui.common.SearchScope
import com.example.thematiclibraryclient.ui.model.BookFilterState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryViewMode {
    SHELVES, ALL_BOOKS
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterState: BookFilterState = BookFilterState(),
    val availableAuthors: List<AuthorDomainModel?> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val filteredBooks: List<BookDomainModel> = emptyList(),
    val shelves: List<ShelfDomainModel> = emptyList(),
    val allBooks: List<BookDomainModel> = emptyList(),
    val viewMode: LibraryViewMode = LibraryViewMode.ALL_BOOKS,
    val currentSearchScope: SearchScope = SearchScope.Everywhere,
    val shelfSearchQuery: String = "",
    val filteredShelves: List<ShelfDomainModel> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getBooksUseCase: GetBooksUseCase,
    private val uploadBookUseCase: UploadBookUseCase,
    private val refreshBooksUseCase: RefreshBooksUseCase,
    private val refreshShelvesUseCase: RefreshShelvesUseCase,
    private val getShelvesUseCase: GetShelvesUseCase,
    private val createShelfUseCase: CreateShelfUseCase,
    private val updateShelfUseCase: UpdateShelfUseCase,
    private val deleteShelfUseCase: DeleteShelfUseCase,
    private val filterEngine: BookFilterEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        subscribeToBooks()
        subscribeToShelves()
        refreshData()
    }

    fun refreshData() {
        refreshBooks()
        refreshShelves()
    }

    private fun subscribeToBooks() {
        viewModelScope.launch {
            getBooksUseCase().collect { books ->
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = refreshBooksUseCase()) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is ConnectionExceptionDomainModel.NoInternet -> "Нет сети. Показаны сохраненные книги."
                        else -> "Ошибка синхронизации."
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
                }
            }
        }
    }

    private fun subscribeToShelves() {
        viewModelScope.launch {
            getShelvesUseCase().collect { shelves ->
                _uiState.value = _uiState.value.copy(
                    shelves = shelves,
                    filteredShelves = shelves
                )
                filterShelves(_uiState.value.shelfSearchQuery)
            }
        }
    }

    fun refreshShelves() {
        viewModelScope.launch {
            refreshShelvesUseCase()
        }
    }

    fun onShelfSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(shelfSearchQuery = query)
        filterShelves(query)
    }

    private fun filterShelves(query: String) {
        val allShelves = _uiState.value.shelves
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(filteredShelves = allShelves)
        } else {
            val filtered = allShelves.filter { shelf ->
                shelf.name.contains(query, ignoreCase = true)
            }
            _uiState.value = _uiState.value.copy(filteredShelves = filtered)
        }
    }

    fun uploadBook(fileUri: Uri){
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isUploading = true, error = null)

            when(val result = uploadBookUseCase.invoke(fileUri)){
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUploading = false)
                    refreshBooks()
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети. Проверьте подключение."
                        is ConnectionExceptionDomainModel.Unauthorized -> "Ошибка авторизации. Попробуйте войти заново."
                        else -> "Произошла неизвестная ошибка."
                    }
                    _uiState.value = _uiState.value.copy(isUploading = false, error = errorMessage)
                }

            }

        }
    }

    fun onSearchScopeChanged(scope: SearchScope) {
        _uiState.value = _uiState.value.copy(currentSearchScope = scope)
        applyFilters()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onFilterChanged(newFilterState: BookFilterState) {
        _uiState.value = _uiState.value.copy(filterState = newFilterState)
        applyFilters()
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

    fun clearFilters() {
        onFilterChanged(BookFilterState())
    }

    fun setViewMode(mode: LibraryViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun createShelf(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                when (val result = createShelfUseCase(name)) {
                    is TResult.Success -> { }
                    is TResult.Error -> {
                        _uiState.value = _uiState.value.copy(error = "Ошибка создания полки")
                    }
                }
            }
        }
    }

    fun updateShelf(shelfId: Int, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                when (updateShelfUseCase(shelfId, newName)) {
                    is TResult.Success -> { }
                    is TResult.Error -> {
                        _uiState.value = _uiState.value.copy(error = "Ошибка обновления полки")
                    }
                }
            }
        }
    }

    fun deleteShelf(shelfId: Int) {
        viewModelScope.launch {
            when (deleteShelfUseCase(shelfId)) {
                is TResult.Success -> { }
                is TResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = "Ошибка удаления полки")
                }
            }
        }
    }

}