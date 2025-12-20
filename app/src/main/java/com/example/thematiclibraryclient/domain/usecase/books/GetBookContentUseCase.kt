package com.example.thematiclibraryclient.domain.usecase.books

import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class GetBookContentUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
  //  suspend operator fun invoke(bookId: Int) = repository.getBookContent(bookId)
}