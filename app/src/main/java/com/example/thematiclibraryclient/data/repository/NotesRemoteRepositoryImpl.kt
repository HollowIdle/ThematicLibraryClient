package com.example.thematiclibraryclient.data.repository

import com.example.thematiclibraryclient.data.local.dao.QuotesDao
import com.example.thematiclibraryclient.data.remote.api.INotesApi
import com.example.thematiclibraryclient.data.remote.model.notes.NoteRequestApiModel
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
        quotesDao.updateNoteContent(quoteId, content)

        val quote = quotesDao.getQuoteEntityById(quoteId)
        if (quote?.serverId != null) {
            try {
                val request = NoteRequestApiModel(content)
                notesApi.upsertNote(quote.serverId, request)
                quotesDao.insertQuote(quote.copy(noteContent = content, isSynced = true))
            } catch (e: Exception) {
            }
        }
        return TResult.Success(NoteDomainModel(content, quoteId))
    }

    override suspend fun syncPendingChanges(): TResult<Unit, Exception> {
        return try {
            val unsyncedQuotes = quotesDao.getUnsyncedQuotes()

            for (quote in unsyncedQuotes) {
                if (quote.serverId != null) {
                    val content = quote.noteContent ?: ""
                    val request = NoteRequestApiModel(content)
                    notesApi.upsertNote(quote.serverId, request)

                    quotesDao.insertQuote(quote.copy(isSynced = true))
                }
            }
            TResult.Success(Unit)
        } catch (e: Exception) {
            TResult.Error(e)
        }
    }

}