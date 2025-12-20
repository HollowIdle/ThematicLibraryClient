package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import javax.inject.Inject

class RefreshBooksUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    suspend operator fun invoke() = repository.refreshBooks()
}