package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.usecase.bookmarks.GetBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.AddBookToShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBookDetailsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.RemoveBookFromShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.GetQuotesForBookUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.GetShelvesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailsUiState(
    val isLoading: Boolean = false,
    val bookDetails: BookDetailsDomainModel? = null,
    val allShelves: List<ShelfDomainModel> = emptyList(),
    val quotes: List<QuoteDomainModel> = emptyList(),
    val bookmarks: List<BookmarkDomainModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    private val getBookDetailsUseCase: GetBookDetailsUseCase,
    private val getShelvesUseCase: GetShelvesUseCase,
    private val addBookToShelfUseCase: AddBookToShelfUseCase,
    private val removeBookFromShelfUseCase: RemoveBookFromShelfUseCase,
    private val getQuotesForBookUseCase: GetQuotesForBookUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: Int = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = BookDetailsUiState(isLoading = true)

            val bookDetailsDeferred = async { getBookDetailsUseCase(bookId) }
            val shelvesDeferred = async { getShelvesUseCase() }
            val quotesDeferred = async { getQuotesForBookUseCase(bookId) }
            val bookmarksDeferred = async { getBookmarksUseCase(bookId) }

            val bookDetailsResult = bookDetailsDeferred.await()
            val shelvesResult = shelvesDeferred.await()
            val quotesResult = quotesDeferred.await()
            val bookmarksResult = bookmarksDeferred.await()

            if (bookDetailsResult is TResult.Success && shelvesResult is TResult.Success) {
                _uiState.value = BookDetailsUiState(
                    bookDetails = bookDetailsResult.data,
                    allShelves = shelvesResult.data,
                    quotes = (quotesResult as? TResult.Success)?.data ?: emptyList(),
                    bookmarks = (bookmarksResult as? TResult.Success)?.data ?: emptyList()
                )
            } else {
                _uiState.value = BookDetailsUiState(error = "Не удалось загрузить данные")
            }
        }
    }

    fun loadBookDetails() {
        viewModelScope.launch {
            _uiState.value = BookDetailsUiState(isLoading = true)
            when (val result = getBookDetailsUseCase(bookId)) {
                is TResult.Success -> {
                    _uiState.value = BookDetailsUiState(bookDetails = result.data)
                }
                is TResult.Error -> {
                    _uiState.value = BookDetailsUiState(error = "Не удалось загрузить детали книги")
                }
            }
        }
    }

    fun onShelfMembershipChanged(shelfId: Int, belongsToShelf: Boolean) {
        viewModelScope.launch {
            val result = if (belongsToShelf) {
                addBookToShelfUseCase(shelfId, bookId)
            } else {
                removeBookFromShelfUseCase(shelfId, bookId)
            }

            if (result is TResult.Success) {
                val updatedShelfIds = _uiState.value.bookDetails?.shelfIds?.toMutableList() ?: mutableListOf()
                if (belongsToShelf) {
                    updatedShelfIds.add(shelfId)
                } else {
                    updatedShelfIds.remove(shelfId)
                }
                _uiState.value = _uiState.value.copy(
                    bookDetails = _uiState.value.bookDetails?.copy(shelfIds = updatedShelfIds)
                )
            } else {
                // TODO: Показать ошибку (например, через eventFlow и Snackbar)
            }
        }
    }
}