package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class UpdateBookDescriptionUseCase @Inject constructor(private val repo: IBooksRemoteRepository) {
    suspend operator fun invoke(bookId: Int, description: String) = repo.updateDescription(bookId, description)
}