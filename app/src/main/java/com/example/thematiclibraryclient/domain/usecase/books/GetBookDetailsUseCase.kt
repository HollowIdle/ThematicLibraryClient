package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookDetailsUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    operator fun invoke(bookId: Int): Flow<BookDetailsDomainModel?> = repository.getBookDetails(bookId)
}