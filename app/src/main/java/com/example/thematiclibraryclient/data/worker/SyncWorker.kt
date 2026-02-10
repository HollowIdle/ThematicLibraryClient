package com.example.thematiclibraryclient.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.repository.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val booksRepository: IBooksRemoteRepository,
    private val shelvesRepository: IShelvesRemoteRepository,
    private val quotesRepository: IQuotesRemoteRepository,
    private val bookmarksRepository: IBookmarksRemoteRepository,
    private val notesRepository: INotesRemoteRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            suspend fun check(stepName: String, action: suspend () -> TResult<Unit, Exception>) {
                val result = action()
                if (result is TResult.Error) {
                    Log.e("SYNC_LOG", "SyncWorker: Ошибка на шаге $stepName: ${result.exception?.message}")
                }
            }

            check("Шаг 1 - Книги (Upload)") { booksRepository.syncPendingChanges() }
            check("Шаг 2 - Полки (Sync)") { shelvesRepository.syncPendingChanges() }
            check("Шаг 3 - Цитаты (Sync)") { quotesRepository.syncPendingChanges() }
            check("Шаг 4 - Заметки (Sync)") { notesRepository.syncPendingChanges() }
            check("Шаг 5 - Закладки (Sync)") { bookmarksRepository.syncPendingChanges() }
            check("Шаг 6 - Refresh Shelves") { shelvesRepository.refreshShelves() as TResult<Unit, Exception> }
            check("Шаг 7 - Refresh Books") { booksRepository.refreshBooks() as TResult<Unit, Exception> }
            check("Шаг 8 - Refresh Quotes") { quotesRepository.refreshQuotes() as TResult<Unit, Exception> }

            Log.d("SYNC_LOG", "SyncWorker: Шаг 9 - Refresh Shelf Contents")
            shelvesRepository.getShelves().firstOrNull()?.forEach { shelf ->
                shelvesRepository.refreshBooksOnShelf(shelf.id)
            }

            Log.d("SYNC_LOG", "SyncWorker: === ЗАВЕРШЕНО ===")
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        var result: T? = null
        try {
            collect {
                result = it
                throw Exception("Abort")
            }
        } catch (e: Exception) { }
        return result
    }
}