package com.example.thematiclibraryclient.domain.repository

import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.NoteDomainModel

interface INotesRemoteRepository {
    suspend fun upsertNote(quoteId: Int, content: String): TResult<NoteDomainModel, ConnectionExceptionDomainModel>

    suspend fun syncPendingChanges(): TResult<Unit, Exception>
}