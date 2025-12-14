package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class DeleteBookUseCase @Inject constructor(private val repo: IBooksRemoteRepository) {
    suspend operator fun invoke(bookId: Int) = repo.deleteBook(bookId)
}