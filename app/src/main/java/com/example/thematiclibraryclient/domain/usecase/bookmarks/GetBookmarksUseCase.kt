package com.example.thematiclibraryclient.domain.usecase.bookmarks

import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import jakarta.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val repository: IBookmarksRemoteRepository
) {
    operator fun invoke(bookId: Int) = repository.getBookmarks(bookId)
}