package com.example.thematiclibraryclient.domain.usecase.books

import android.net.Uri
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import javax.inject.Inject

class UploadBookUseCase @Inject constructor(
    private val booksRemoteRepository: IBooksRemoteRepository
) {
    suspend operator fun invoke(fileUri: Uri) = booksRemoteRepository.uploadBook(fileUri)
}