package com.example.thematiclibraryclient.domain.usecase.quotes

import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import javax.inject.Inject

class CreateQuoteUseCase @Inject constructor(
    private val repository: IQuotesRemoteRepository
) {
    suspend operator fun invoke(bookId: Int, text: String, positionStart: Int, positionEnd: Int, note: String?, locatorData: String? = null) =
        repository.createQuote(bookId, text, start = positionStart, end = positionEnd, note = note, locatorData)
}