package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import javax.inject.Inject

class RefreshBookDetailsUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    suspend operator fun invoke(bookId: Int) = repository.refreshBookDetails(bookId)
}