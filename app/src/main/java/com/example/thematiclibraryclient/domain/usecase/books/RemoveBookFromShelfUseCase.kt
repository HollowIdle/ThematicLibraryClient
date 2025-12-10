package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import jakarta.inject.Inject

class RemoveBookFromShelfUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    suspend operator fun invoke(shelfId: Int, bookId: Int) = repository.removeBookFromShelf(shelfId, bookId)
}