package com.example.thematiclibraryclient.domain.usecase.quotes
import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import javax.inject.Inject

class RefreshQuotesUseCase @Inject constructor (
    private val repository: IQuotesRemoteRepository
) {
    suspend operator fun invoke() = repository.refreshQuotes()
}