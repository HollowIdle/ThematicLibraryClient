package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBooksUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    operator fun invoke(): Flow<List<BookDomainModel>> = repository.getBooks()
}