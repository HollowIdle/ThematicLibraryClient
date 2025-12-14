package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class UpdateBookProgressUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    suspend operator fun invoke(bookId: Int, position: Int) =
        repository.updateProgress(bookId, position)
}