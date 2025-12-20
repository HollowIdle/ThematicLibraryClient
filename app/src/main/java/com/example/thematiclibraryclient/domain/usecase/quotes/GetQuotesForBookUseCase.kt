package com.example.thematiclibraryclient.domain.usecase.quotes

import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import jakarta.inject.Inject

class GetQuotesForBookUseCase @Inject constructor (
    private val repository: IQuotesRemoteRepository
) {
    operator fun invoke(bookId: Int) = repository.getQuotesForBook(bookId)
}