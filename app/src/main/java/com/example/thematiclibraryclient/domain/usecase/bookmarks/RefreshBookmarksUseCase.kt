package com.example.thematiclibraryclient.domain.usecase.bookmarks
import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import javax.inject.Inject

class RefreshBookmarksUseCase @Inject constructor (
    private val repository: IBookmarksRemoteRepository
) {
    suspend operator fun invoke(bookId: Int) = repository.refreshBookmarks(bookId)
}