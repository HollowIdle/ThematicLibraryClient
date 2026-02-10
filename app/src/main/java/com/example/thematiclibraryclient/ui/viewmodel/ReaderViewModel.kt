package com.example.thematiclibraryclient.ui.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.data.local.source.BookContentCache
import com.example.thematiclibraryclient.data.local.source.PaginationCache
import com.example.thematiclibraryclient.domain.common.DocxToPdfConverter
import com.example.thematiclibraryclient.domain.common.LocalBookParser
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.usecase.bookmarks.CreateBookmarkUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.DeleteBookmarkUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.GetBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.bookmarks.RefreshBookmarksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.DownloadBookUseCase
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
    val requestedPage: Int? = null,
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
    private val downloadBookUseCase: DownloadBookUseCase,
    private val bookContentCache: BookContentCache,
    val paginationCache: PaginationCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var targetGlobalPosition: Int = -1
    private var targetPageIndex: Int = -1
    private var isInitialNavigationDone = false

    fun loadBook(bookId: Int, navPosition: Int = -1, isPageNavigation: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val details = getBookDetailsUseCase(bookId).firstOrNull()
            if (details == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Ошибка загрузки инфо")
                return@launch
            }
            _uiState.value = _uiState.value.copy(bookTitle = details.title)

            isInitialNavigationDone = false
            targetGlobalPosition = -1
            targetPageIndex = -1

            if (navPosition != -1) {
                if (isPageNavigation) {
                    targetPageIndex = navPosition
                } else {
                    targetGlobalPosition = navPosition
                }
            } else {
                targetGlobalPosition = details.lastPosition
            }

            launch {
                getBookmarksUseCase(bookId).collect { bookmarks ->
                    _uiState.value = _uiState.value.copy(bookmarks = bookmarks)
                }
            }
            launch { refreshBookmarksUseCase(bookId) }

            val cachedHtml = bookContentCache.loadContent(bookId)
            if (cachedHtml != null) {
                processHtmlContent(cachedHtml)
                return@launch
            }

            when (val result = downloadBookUseCase(bookId, "book_file")) {
                is TResult.Success -> {
                    val originalFile = result.data
                    val extension = originalFile.extension.lowercase()

                    try {
                        when (extension) {
                            "pdf" -> {
                                val page = if (targetPageIndex != -1) targetPageIndex
                                else if (targetGlobalPosition != -1) targetGlobalPosition
                                else 0

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPdfMode = true,
                                    pdfPath = originalFile.absolutePath,
                                    currentPage = page,
                                    requestedPage = page
                                )
                            }
                            "docx" -> {
                                val pdfFile = docxToPdfConverter.convert(originalFile)
                                val page = if (targetPageIndex != -1) targetPageIndex
                                else if (targetGlobalPosition != -1) targetGlobalPosition
                                else 0

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isPdfMode = true,
                                    pdfPath = pdfFile.absolutePath,
                                    currentPage = page,
                                    requestedPage = page
                                )
                            }
                            else -> {
                                val htmlContent = withContext(Dispatchers.Default) {
                                    localBookParser.parseToHtml(originalFile)
                                }
                                bookContentCache.saveContent(bookId, htmlContent)
                                processHtmlContent(htmlContent)
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Ошибка обработки: ${e.message}"
                        )
                    }
                }
                is TResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Ошибка загрузки файла")
                }
            }
        }
    }

    private suspend fun processHtmlContent(html: String) {
        val formattedContent = parseHtmlToAnnotatedString(html)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isPdfMode = false,
            fullContent = formattedContent
        )
    }

    fun onPagesUpdated(pages: List<AnnotatedString>) {
        val currentState = _uiState.value

        if (pages.size == currentState.pages.size && pages.isNotEmpty()) return

        var newCurrentPage = currentState.currentPage

        if (!isInitialNavigationDone && pages.isNotEmpty()) {

            if (targetPageIndex != -1) {
                if (pages.size > targetPageIndex) {
                    newCurrentPage = targetPageIndex
                    isInitialNavigationDone = true
                    targetPageIndex = -1
                }
            } else if (targetGlobalPosition >= 0) {
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
                    targetGlobalPosition = -1
                } else {
                    val totalTextLength = pages.sumOf { it.length }
                    val fullLength = currentState.fullContent?.length ?: 0
                    if (fullLength > 0 && totalTextLength >= fullLength) {
                        newCurrentPage = pages.lastIndex
                        isInitialNavigationDone = true
                        targetGlobalPosition = -1
                    }
                }
            }
        }

        _uiState.value = currentState.copy(
            pages = pages,
            currentPage = newCurrentPage
        )
    }

    fun onPageChanged(page: Int) {
        if (_uiState.value.currentPage != page) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
        if (_uiState.value.requestedPage != null &&
            (_uiState.value.requestedPage == page || _uiState.value.requestedPage == page + 1 || _uiState.value.requestedPage == page - 1)) {
            _uiState.value = _uiState.value.copy(requestedPage = null)
        }
    }

    fun jumpToPage(page: Int) {
        _uiState.value = _uiState.value.copy(
            currentPage = page,
            requestedPage = page
        )
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

    /*fun toggleBookmark(bookId: Int) {
        val currentState = _uiState.value
        val currentPage = currentState.currentPage
        val existingBookmark = currentState.bookmarks.find { it.position == currentPage }

        if (existingBookmark != null) {
            deleteBookmark(existingBookmark)
        } else {
            createBookmark(bookId, currentPage)
        }
    }*/

    fun createBookmark(bookId: Int, pageIndex: Int, note: String?) {
        viewModelScope.launch {
            when (createBookmarkUseCase(bookId, pageIndex, note)) {
                is TResult.Success -> _eventFlow.emit(UiEvent.ShowSnackbar("Закладка добавлена"))
                is TResult.Error -> _eventFlow.emit(UiEvent.ShowSnackbar("Ошибка"))
            }
        }
    }

    fun deleteBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            when (deleteBookmarkUseCase(bookmarkId)) {
                is TResult.Success -> {
                    val updated = _uiState.value.bookmarks.filterNot { it.id == bookmarkId }
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