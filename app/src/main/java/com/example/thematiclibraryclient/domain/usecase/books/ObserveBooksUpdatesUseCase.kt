package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBooksUpdatesUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    suspend operator fun invoke() : Flow<Unit> = repository.booksUpdateFlow
}