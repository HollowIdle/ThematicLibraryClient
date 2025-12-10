package com.example.thematiclibraryclient.domain.usecase.bookmarks

import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import jakarta.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val repository: IBookmarksRemoteRepository
) {
    suspend operator fun invoke(bookId: Int) = repository.getBookmarksForBook(bookId)
}