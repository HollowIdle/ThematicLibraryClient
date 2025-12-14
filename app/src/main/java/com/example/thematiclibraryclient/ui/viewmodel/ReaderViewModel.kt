package com.example.thematiclibraryclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.usecase.bookmarks.CreateBookmarkUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.CreateQuoteUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.DeleteBookmarkUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBookContentUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.GetBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBookDetailsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ReaderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookTitle: String = "",
    val fullContent: String? = null,
    val pages: List<String> = emptyList(),
    val currentPage: Int = 0,
    val isInSelectionMode: Boolean = false,
    val bookmarks: List<BookmarkDomainModel> = emptyList(),
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getBookContentUseCase: GetBookContentUseCase,
    private val createQuoteUseCase: CreateQuoteUseCase,
    private val createBookmarkUseCase: CreateBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val getBookDetailsUseCase: GetBookDetailsUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var initialGlobalPosition: Int = 0

    fun loadBook(bookId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val detailsResult = getBookDetailsUseCase(bookId)
            if (detailsResult is TResult.Success) {
                initialGlobalPosition = detailsResult.data.lastPosition
            }

            when (val result = getBookContentUseCase(bookId)) {
                is TResult.Success -> {
                    val bookmarksResult = getBookmarksUseCase(bookId)
                    val bookmarks = if (bookmarksResult is TResult.Success) {
                        bookmarksResult.data
                    } else {
                        emptyList()
                    }

                    val preprocessedText = preprocessText(result.data)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        fullContent = preprocessedText,
                        bookmarks = bookmarks
                    )
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети."
                        is ConnectionExceptionDomainModel.Unauthorized -> "Ошибка авторизации."
                        else -> "Произошла неизвестная ошибка."
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
                }
            }
        }
    }

    private fun preprocessText(rawText: String): String {
        val paragraphBreakMarker = "[[PARAGRAPH_BREAK]]"

        val textWithUnixEndings = rawText.replace("\r\n", "\n")

        val textWithMarkers = textWithUnixEndings.replace(Regex("\n{2,}"), paragraphBreakMarker)

        val singleLineParagraphs = textWithMarkers.replace('\n', ' ')

        val paragraphIndent = "\u0020\u0020\u0020\u0020"
        val textWithIndents = singleLineParagraphs.replace(paragraphBreakMarker, "\n$paragraphIndent")

        val final_text = if (!textWithIndents.startsWith(paragraphIndent) && textWithIndents.isNotEmpty()) {
            paragraphIndent + textWithIndents
        } else {
            textWithIndents
        }

        return final_text.trim()
    }

    fun onPagesCalculated(pages: List<String>) {
        var targetPage = 0
        if (initialGlobalPosition > 0 && pages.isNotEmpty()) {
            var charCount = 0
            for ((index, page) in pages.withIndex()) {
                if (initialGlobalPosition < charCount + page.length) {
                    targetPage = index
                    break
                }
                charCount += page.length
            }
        }

        _uiState.value = _uiState.value.copy(
            pages = pages,
            currentPage = targetPage
        )
    }

    fun onPageChanged(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    fun saveProgress(bookId: Int) {
        val currentState = _uiState.value
        if (currentState.pages.isEmpty()) return

        val currentPageIndex = currentState.currentPage
        val currentGlobalOffset = currentState.pages.take(currentPageIndex).sumOf { it.length }

        viewModelScope.launch {
            withContext(NonCancellable){
                try {
                    updateBookProgressUseCase(bookId, currentGlobalOffset)
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    fun toggleBookmark(bookId: Int) {
        val currentState = _uiState.value
        val currentPage = currentState.currentPage

        val existingBookmark = currentState.bookmarks.find { it.position == currentPage }

        if (existingBookmark != null) {
            deleteBookmark(existingBookmark)
        } else {
            createBookmark(bookId, currentPage)
        }
    }

    fun createQuote(bookId: Int, text: String, positionStart: Int, positionEnd: Int, note: String?) {
        val currentState = _uiState.value
        val currentPageIndex = currentState.currentPage
        val offset = currentState.pages.take(currentPageIndex).sumOf { it.length }
        val globalStart = offset + positionStart
        val globalEnd = offset + positionEnd

        viewModelScope.launch {
            when (createQuoteUseCase(bookId, text, globalStart, globalEnd, note)) {
                is TResult.Success -> _eventFlow.emit(UiEvent.ShowSnackbar("Цитата успешно создана!"))
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка при создании цитаты"))
            }
        }
    }

    private fun createBookmark(bookId: Int, pageIndex: Int) {
        viewModelScope.launch {
            when (val result = createBookmarkUseCase(bookId, pageIndex, null)) {
                is TResult.Success -> {
                    val newBookmark = result.data
                    val updatedBookmarks = _uiState.value.bookmarks + newBookmark
                    _uiState.value = _uiState.value.copy(bookmarks = updatedBookmarks)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Закладка добавлена на стр. ${pageIndex + 1}"))
                }
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка добавления закладки"))
            }
        }
    }

    private fun deleteBookmark(bookmark: BookmarkDomainModel) {
        viewModelScope.launch {
            when (deleteBookmarkUseCase(bookmark.id)) {
                is TResult.Success -> {
                    val updatedBookmarks = _uiState.value.bookmarks.filterNot { it.id == bookmark.id }
                    _uiState.value = _uiState.value.copy(bookmarks = updatedBookmarks)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Закладка удалена со стр. ${bookmark.position + 1}"))
                }
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка удаления закладки"))
            }
        }
    }

    fun toggleSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isInSelectionMode = !_uiState.value.isInSelectionMode
        )
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}