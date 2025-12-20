package com.example.thematiclibraryclient.domain.usecase.books

import android.net.Uri
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import jakarta.inject.Inject

class AddLocalBookUseCase @Inject constructor(
    private val repository: IBooksRemoteRepository
) {
    suspend operator fun invoke(uri: Uri) = repository.addLocalBook(uri)
}