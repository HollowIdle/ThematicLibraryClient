package com.example.thematiclibraryclient.domain.usecase.notes

import com.example.thematiclibraryclient.domain.repository.INotesRemoteRepository
import jakarta.inject.Inject

class UpsertNoteUseCase @Inject constructor (
    private val repository: INotesRemoteRepository
) {
    suspend operator fun invoke(quoteId: Int, content: String) = repository.upsertNote(quoteId, content)
}