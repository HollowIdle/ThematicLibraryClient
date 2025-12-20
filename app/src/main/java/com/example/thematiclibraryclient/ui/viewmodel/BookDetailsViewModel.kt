package com.example.thematiclibraryclient.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.usecase.bookmarks.GetBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.RefreshBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.AddBookToShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.books.DeleteBookUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBookDetailsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.RefreshBookDetailsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.RemoveBookFromShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UpdateBookAuthorsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UpdateBookTagsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UpdateBookDescriptionUseCase
import com.example.thematiclibraryclient.domain.usecase.notes.UpsertNoteUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.DeleteQuoteUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.GetQuotesForBookUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.GetShelvesUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.RefreshBooksOnShelfUseCase
import com.example.thematiclibraryclient.domain.usecase.shelves.RefreshShelvesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailsUiState(
    val isLoading: Boolean = false,
    val bookDetails: BookDetailsDomainModel? = null,
    val allShelves: List<ShelfDomainModel> = emptyList(),
    val quotes: List<QuoteDomainModel> = emptyList(),
    val bookmarks: List<BookmarkDomainModel> = emptyList(),
    val selectedQuote: QuoteDomainModel? = null,
    val error: String? = null
)

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    private val getBookDetailsUseCase: GetBookDetailsUseCase,
    private val addBookToShelfUseCase: AddBookToShelfUseCase,
    private val removeBookFromShelfUseCase: RemoveBookFromShelfUseCase,
    private val updateBookAuthorsUseCase: UpdateBookAuthorsUseCase,
    private val updateBookTagsUseCase: UpdateBookTagsUseCase,
    private val updateBookDescriptionUseCase: UpdateBookDescriptionUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val deleteQuoteUseCase: DeleteQuoteUseCase,
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val refreshBookDetailsUseCase: RefreshBookDetailsUseCase,
    private val getShelvesUseCase: GetShelvesUseCase,
    private val refreshShelvesUseCase: RefreshShelvesUseCase,
    private val getQuotesForBookUseCase: GetQuotesForBookUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val refreshBookmarksUseCase: RefreshBookmarksUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: Int = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BookDetailsViewModel.UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        subscribeToBookDetails()
        subscribeToShelves()
        subscribeToQuotes()
        subscribeToBookmarks()
        refreshBookDetails()
        refreshShelves()
        refreshBookmarks()
    }

    private fun subscribeToBookmarks() {
        viewModelScope.launch {
            getBookmarksUseCase(bookId).collect { bookmarks ->
                _uiState.value = _uiState.value.copy(bookmarks = bookmarks)
            }
        }
    }

    private fun refreshBookmarks() {
        viewModelScope.launch {
            refreshBookmarksUseCase(bookId)
        }
    }

    private fun subscribeToQuotes() {
        viewModelScope.launch {
            getQuotesForBookUseCase(bookId).collect { quotes ->
                _uiState.value = _uiState.value.copy(quotes = quotes)
            }
        }
    }

    private fun subscribeToShelves() {
        viewModelScope.launch {
            getShelvesUseCase().collect { shelves ->
                _uiState.value = _uiState.value.copy(allShelves = shelves)
            }
        }
    }

    private fun refreshShelves() {
        viewModelScope.launch {
            refreshShelvesUseCase()
        }
    }

    private fun subscribeToBookDetails() {
        viewModelScope.launch {
            getBookDetailsUseCase(bookId).collect { details ->
                if (details != null) {
                    _uiState.value = _uiState.value.copy(bookDetails = details)
                }
            }
        }
    }

    fun refreshBookDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = refreshBookDetailsUseCase(bookId)) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is TResult.Error -> {
                    val errorMsg = if (_uiState.value.bookDetails == null) "Не удалось загрузить книгу" else "Работаем оффлайн"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            }
        }
    }


    fun updateAuthors(authors: List<String>) {
        viewModelScope.launch {
            when (updateBookAuthorsUseCase(bookId, authors)) {
                is TResult.Success -> { }
                is TResult.Error -> { _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка при обновлении списка авторов")) }
            }
        }
    }

    fun updateTags(tags: List<String>) {
        viewModelScope.launch {
            when (updateBookTagsUseCase(bookId, tags)) {
                is TResult.Success -> { }
                is TResult.Error -> { _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка при обновлении списка тегов")) }
            }
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            when (updateBookDescriptionUseCase(bookId, description)) {
                is TResult.Success -> { }
                is TResult.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка при обновлении описания книги"))
                }
            }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            when (deleteBookUseCase(bookId)) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(selectedQuote = null)
                    _eventFlow.emit(UiEvent.NavigateBack)
                }
                is TResult.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка при удалении книги"))
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

            if (result !is TResult.Success) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка при изменении нахождения книги на полке"))
            }
        }
    }

    fun onQuoteSelected(quote: QuoteDomainModel) {
        _uiState.value = _uiState.value.copy(selectedQuote = quote)
    }

    fun onDialogDismiss() {
        _uiState.value = _uiState.value.copy(selectedQuote = null)
    }

    fun deleteQuote(quoteToDelete: QuoteDomainModel) {
        viewModelScope.launch {
            when (deleteQuoteUseCase(quoteToDelete.id)) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(selectedQuote = null)
                }
                is TResult.Error -> {  }
            }
        }
    }

    fun saveNoteForSelectedQuote(noteContent: String) {
        val quoteToUpdate = _uiState.value.selectedQuote ?: return
        viewModelScope.launch {
            when (upsertNoteUseCase(quoteToUpdate.id, noteContent)) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(selectedQuote = null)
                }
                is TResult.Error -> {  }
            }
        }
    }

    sealed class UiEvent {
        object NavigateBack : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    var selectedTabIndex by mutableStateOf(0)
        private set

    fun onTabSelected(index: Int) {
        selectedTabIndex = index
    }
}