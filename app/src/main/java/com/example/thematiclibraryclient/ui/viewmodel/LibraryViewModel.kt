package com.example.thematiclibraryclient.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.usecase.books.GetBooksUseCase
import com.example.thematiclibraryclient.domain.usecase.books.UploadBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val books: List<BookDomainModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getBooksUseCase: GetBooksUseCase,
    private val uploadBookUseCase: UploadBookUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState(isLoading = true)

            when (val result = getBooksUseCase()) {
                is TResult.Success -> {
                    _uiState.value = LibraryUiState(books = result.data)
                }
                is TResult.Error -> {
                    val errorMessage = when (result.exception) {
                        is ConnectionExceptionDomainModel.NoInternet -> "Ошибка сети. Проверьте подключение."
                        is ConnectionExceptionDomainModel.Unauthorized -> "Ошибка авторизации. Попробуйте войти заново."
                        else -> "Произошла неизвестная ошибка."
                    }
                    _uiState.value = LibraryUiState(error = errorMessage)
                }
            }
        }
    }

    fun uploadBook(fileUri: Uri){
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isUploading = true, error = null)

            when(val result = uploadBookUseCase.invoke(fileUri)){
                is TResult.Success -> {
                    loadBooks()

                    _uiState.value = _uiState.value.copy(isUploading = false)


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

}