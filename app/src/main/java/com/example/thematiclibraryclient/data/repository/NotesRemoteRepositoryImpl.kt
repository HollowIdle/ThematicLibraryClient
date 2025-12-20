package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.QuotesDao
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.INotesApi
import com.example.thematiclibraryclient.data.remote.model.notes.NoteRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.notes.toDomainModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.NoteDomainModel
import com.example.thematiclibraryclient.domain.repository.INotesRemoteRepository
import jakarta.inject.Inject


class NotesRemoteRepositoryImpl @Inject constructor(
    private val notesApi: INotesApi,
    private val quotesDao: QuotesDao
) : INotesRemoteRepository {

    override suspend fun upsertNote(
        quoteId: Int,
        content: String
    ): TResult<NoteDomainModel, ConnectionExceptionDomainModel> {
        return try {
            val request = NoteRequestApiModel(content)
            val response = notesApi.upsertNote(quoteId, request)

            quotesDao.updateNoteContent(quoteId, content)

            TResult.Success(response.toDomainModel())
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

}