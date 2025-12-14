package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class UpdateBookAuthorsUseCase @Inject constructor(private val repo: IBooksRemoteRepository) {
    suspend operator fun invoke(bookId: Int, authors: List<String>) = repo.updateAuthors(bookId, authors)
}