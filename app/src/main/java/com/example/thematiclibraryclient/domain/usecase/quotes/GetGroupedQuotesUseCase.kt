package com.example.thematiclibraryclient.domain.usecase.quotes

import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import jakarta.inject.Inject

class GetGroupedQuotesUseCase @Inject constructor(
    private val repository: IQuotesRemoteRepository
) {
    operator fun invoke() = repository.getGroupedQuotes()
}