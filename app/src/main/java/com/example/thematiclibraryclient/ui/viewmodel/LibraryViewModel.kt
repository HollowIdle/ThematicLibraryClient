package com.example.thematiclibraryclient.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.data.common.NetworkConnectivityObserver
import com.example.thematiclibraryclient.domain.common.LocalBookParser
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.domain.usecase.books.AddLocalBookUseCase
import com.example.thematiclibraryclient.domain.usecase.books.GetBooksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.RefreshBooksUseCase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val duplicateBookUri: Uri? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getBooksUseCase: GetBooksUseCase,
    private val addLocalBookUseCase: AddLocalBookUseCase,
    private val refreshBooksUseCase: RefreshBooksUseCase,
    private val refreshShelvesUseCase: RefreshShelvesUseCase,
    private val getShelvesUseCase: GetShelvesUseCase,
    private val createShelfUseCase: CreateShelfUseCase,
    private val updateShelfUseCase: UpdateShelfUseCase,
    private val deleteShelfUseCase: DeleteShelfUseCase,
    private val filterEngine: BookFilterEngine,
    private val localBookParser: LocalBookParser,
    private val networkObserver: NetworkConnectivityObserver
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
            val hasData = _uiState.value.allBooks.isNotEmpty()

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

            when (val result = refreshBooksUseCase()) {
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
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
            val isOnline = networkObserver.observe().first()
            if (!isOnline) return@launch

            refreshShelvesUseCase()
        }
    }

    fun onShelfSearchQueryChanged(query: String) {
        val trimmed = query.trimStart()
        _uiState.value = _uiState.value.copy(shelfSearchQuery = trimmed)
        filterShelves(trimmed.trim())
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

    private fun String.normalizeTitle(): String {
        return this.trim().replace("\\s+".toRegex(), " ").lowercase()
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: ""
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1

        if (longer.isEmpty()) return 1.0

        val distance = levenshteinDistance(longer, shorter)
        return (longer.length - distance).toDouble() / longer.length.toDouble()
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    fun checkAndUploadBook(fileUri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploading = true,
                error = null,
                duplicateBookUri = null
            )

            try {
                val fileNameWithExt = getFileName(context, fileUri)
                val originalCleanName = fileNameWithExt.substringBeforeLast(".")

                val extension = if (fileNameWithExt.contains(".")) {
                    "." + fileNameWithExt.substringAfterLast(".")
                } else {
                    null
                }

                val tempFile = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(fileUri)
                        ?: throw Exception("Не удалось открыть файл")
                    val temp = File.createTempFile("temp_check", extension, context.cacheDir)
                    inputStream.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    temp
                }

                val metadata = withContext(Dispatchers.Default) {
                    localBookParser.parseMetadata(tempFile)
                }

                val tempFileNameWithoutExt = tempFile.nameWithoutExtension
                tempFile.delete()

                val titleFromParser = metadata.title
                val targetTitle = if (titleFromParser.isBlank() || titleFromParser == tempFileNameWithoutExt || titleFromParser.startsWith("temp_check")) {
                    originalCleanName
                } else {
                    titleFromParser
                }

                val normalizedTarget = targetTitle.normalizeTitle()

                val duplicateCandidate = _uiState.value.allBooks.find { existingBook ->

                    val cleanExistingTitleRaw = existingBook.title.replace(Regex("^\\d+_"), "")
                    val existingTitle = cleanExistingTitleRaw.normalizeTitle()

                    val exactMatch = existingTitle == normalizedTarget

                    val similarity = calculateSimilarity(existingTitle, normalizedTarget)

                    exactMatch || similarity > 0.85
                }

                if (duplicateCandidate != null) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        duplicateBookUri = fileUri
                    )
                } else {
                    uploadBook(fileUri)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "Ошибка проверки файла: ${e.message}"
                )
            }
        }
    }

    fun confirmUploadDuplicate() {
        val uri = _uiState.value.duplicateBookUri
        if (uri != null) {
            uploadBook(uri)
            _uiState.value = _uiState.value.copy(duplicateBookUri = null)
        }
    }

    fun cancelUploadDuplicate() {
        _uiState.value = _uiState.value.copy(duplicateBookUri = null)
    }

    fun uploadBook(fileUri: Uri){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)

            when(val result = addLocalBookUseCase(fileUri)){
                is TResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUploading = false)

                }
                is TResult.Error -> {
                    _uiState.value = _uiState.value.copy(isUploading = false, error = "Ошибка добавления книги: ${result.exception?.message}")
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
