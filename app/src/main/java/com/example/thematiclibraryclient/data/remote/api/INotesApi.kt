package com.example.thematiclibraryclient.data.remote.api

import com.example.thematiclibraryclient.data.remote.model.notes.NoteRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.notes.NoteApiModel
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface INotesApi {
    @POST("/api/quotes/{quoteId}/notes")
    suspend fun upsertNote(
        @Path("quoteId") quoteId: Int,
        @Body request: NoteRequestApiModel
    ): NoteApiModel
}