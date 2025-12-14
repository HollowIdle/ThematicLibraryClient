package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class UpdateBookTagsUseCase @Inject constructor(private val repo: IBooksRemoteRepository) {
    suspend operator fun invoke(bookId: Int, tags: List<String>) = repo.updateTags(bookId, tags)
}