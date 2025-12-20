package com.example.thematiclibraryclient.domain.usecase.shelves

import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetBooksOnShelfUseCase @Inject constructor(
    private val repository: IShelvesRemoteRepository
) {
    operator fun invoke(shelfId: Int) : Flow<List<BookDomainModel>> =
        repository.getBooksOnShelf(shelfId)
}