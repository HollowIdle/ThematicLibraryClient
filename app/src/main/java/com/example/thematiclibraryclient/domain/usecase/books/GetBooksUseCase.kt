package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class GetBooksUseCase @Inject constructor(
    private val booksRemoteRepository: IBooksRemoteRepository
) {
    suspend operator fun invoke() = booksRemoteRepository.getBooks()
}