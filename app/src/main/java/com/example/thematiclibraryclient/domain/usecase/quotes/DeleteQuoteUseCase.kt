package com.example.thematiclibraryclient.domain.usecase.quotes

import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import jakarta.inject.Inject

class DeleteQuoteUseCase @Inject constructor(private val repo: IQuotesRemoteRepository) {
    suspend operator fun invoke(quoteId: Int) = repo.deleteQuote(quoteId)
}
