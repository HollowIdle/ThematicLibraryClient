package com.example.thematiclibraryclient.ui.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.DocxToPdfConverter
import com.example.thematiclibraryclient.domain.common.LocalBookParser
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.usecase.bookmarks.CreateBookmarkUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.DeleteBookmarkUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.GetBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.RefreshBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.DownloadBookUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBookContentUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBookDetailsUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UpdateBookProgressUseCase
import com.example.thematiclibraryclient.domain.usecase.quotes.CreateQuoteUseCase
import com.example.thematiclibraryclient.ui.common.parseHtmlToAnnotatedString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ReaderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookTitle: String = "",

    val fullContent: AnnotatedString? = null,
    val pages: List<AnnotatedString> = emptyList(),

    val isPdfMode: Boolean = false,
    val pdfPath: String? = null,

    val currentPage: Int = 0,
    val isInSelectionMode: Boolean = false,
    val bookmarks: List<BookmarkDomainModel> = emptyList(),
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val createQuoteUseCase: CreateQuoteUseCase,
    private val createBookmarkUseCase: CreateBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val getBookDetailsUseCase: GetBookDetailsUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val refreshBookmarksUseCase: RefreshBookmarksUseCase,
    private val localBookParser: LocalBookParser,
    private val docxToPdfConverter: DocxToPdfConverter,
    private val downloadBookUseCase: DownloadBookUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var initialGlobalPosition: Int = 0
    private var targetGlobalPosition: Int = -1
    private var isInitialNavigationDone = false

    fun loadBook(bookId: Int, navPosition: Int = -1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val details = getBookDetailsUseCase(bookId).firstOrNull()
            if (details == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Ошибка загрузки инфо")
                return@launch
            }
            _uiState.value = _uiState.value.copy(bookTitle = details.title)

            targetGlobalPosition = if (navPosition != -1) navPosition else details.lastPosition
            initialGlobalPosition = details.lastPosition

            launch {
                getBookmarksUseCase(bookId).collect { bookmarks ->
                    _uiState.value = _uiState.value.copy(bookmarks = bookmarks)
                }
            }
            launch { refreshBookmarksUseCase(bookId) }

            when (val result = downloadBookUseCase(bookId, "book_file")) {
                is TResult.Success -> {
                    val originalFile = result.data
                    val extension = originalFile.extension.lowercase()

                    try {
                        when (extension) {
                            "pdf" -> {
                                // --- PDF ---
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPdfMode = true,
                                    pdfPath = originalFile.absolutePath,
                                    currentPage = initialGlobalPosition
                                )
                            }
                            "docx" -> {
                                // --- DOCX -> PDF ---
                                // Конвертируем и получаем путь к новому PDF файлу
                                val pdfFile = docxToPdfConverter.convert(originalFile)

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPdfMode = true, // Включаем режим PDF!
                                    pdfPath = pdfFile.absolutePath,
                                    currentPage = initialGlobalPosition
                                )
                            }
                            else -> {
                                // --- EPUB, FB2, TXT (Текстовый режим) ---
                                val htmlContent = withContext(Dispatchers.Default) {
                                    localBookParser.parseToHtml(originalFile)
                                }
                                val formattedContent = parseHtmlToAnnotatedString(htmlContent)

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPdfMode = false,
                                    fullContent = formattedContent
                                )
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Ошибка обработки файла: ${e.message}"
                        )
                        e.printStackTrace()
                    }
                }
                is TResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Ошибка загрузки")
                }
            }
        }
    }

    fun onPagesUpdated(pages: List<AnnotatedString>) {
        val currentState = _uiState.value
        if (pages.size == currentState.pages.size) return

        var newCurrentPage = currentState.currentPage

        if (targetGlobalPosition >= 0 && !isInitialNavigationDone && pages.isNotEmpty()) {
            var currentTotalLength = 0
            var foundPage = -1
            for ((index, page) in pages.withIndex()) {
                val pageEnd = currentTotalLength + page.length
                if (targetGlobalPosition < pageEnd) {
                    foundPage = index
                    break
                }
                currentTotalLength += page.length
            }
            if (foundPage != -1) {
                newCurrentPage = foundPage
                isInitialNavigationDone = true
            }
        }

        _uiState.value = currentState.copy(
            pages = pages,
            currentPage = newCurrentPage,
            isLoading = false
        )
    }

    fun onPageChanged(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    fun saveProgress(bookId: Int) {
        val currentState = _uiState.value
        val positionToSave = if (currentState.isPdfMode) {
            currentState.currentPage
        } else {
            if (currentState.pages.isEmpty()) return
            currentState.pages.take(currentState.currentPage).sumOf { it.length }
        }

        viewModelScope.launch {
            withContext(NonCancellable) {
                updateBookProgressUseCase(bookId, positionToSave)
            }
        }
    }

    fun createQuote(bookId: Int, text: String, start: Int = 0, end: Int = 0, note: String?) {
        val currentState = _uiState.value

        var finalStart = start
        var finalEnd = end

        if (!currentState.isPdfMode) {
            val offset = currentState.pages.take(currentState.currentPage).sumOf { it.length }
            finalStart += offset
            finalEnd += offset
        } else {
            finalStart = currentState.currentPage
            finalEnd = 0
        }

        viewModelScope.launch {
            when (createQuoteUseCase(bookId, text, finalStart, finalEnd, note)) {
                is TResult.Success -> _eventFlow.emit(UiEvent.ShowSnackbar("Цитата создана"))
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка"))
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

    private fun createBookmark(bookId: Int, pageIndex: Int) {
        viewModelScope.launch {
            when (createBookmarkUseCase(bookId, pageIndex, null)) {
                is TResult.Success -> _eventFlow.emit(UiEvent.ShowSnackbar("Закладка добавлена"))
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка"))
            }
        }
    }

    private fun deleteBookmark(bookmark: BookmarkDomainModel) {
        viewModelScope.launch {
            when (deleteBookmarkUseCase(bookmark.id)) {
                is TResult.Success -> {
                    val updated = _uiState.value.bookmarks.filterNot { it.id == bookmark.id }
                    _uiState.value = _uiState.value.copy(bookmarks = updated)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Закладка удалена"))
                }
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка"))
            }
        }
    }

    fun toggleSelectionMode() {
        _uiState.value = _uiState.value.copy(isInSelectionMode = !_uiState.value.isInSelectionMode)
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}