package com.example.thematiclibraryclient.domain.usecase.bookmarks

import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import jakarta.inject.Inject

class DeleteBookmarkUseCase @Inject constructor(
    private val repository: IBookmarksRemoteRepository
) {
    suspend operator fun invoke(bookmarkId: Int) = repository.deleteBookmark(bookmarkId)
}