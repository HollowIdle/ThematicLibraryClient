package com.example.thematiclibraryclient.domain.usecase.auth

import android.content.Context
import com.example.thematiclibraryclient.data.local.db.AppDatabase
import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.io.File

class LogoutUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenLocalDataSource: ITokenLocalDataSource,
    private val appDatabase: AppDatabase
) {
    suspend operator fun invoke() {
        tokenLocalDataSource.clearToken()

        appDatabase.clearDatabase()


        // Delete book files
        val booksDir = File(context.filesDir, "books")
        if (booksDir.exists()) {
            booksDir.deleteRecursively()
        }

        // Delete pagination cache
        val cacheDir = File(context.cacheDir, "pagination_cache")
        if (cacheDir.exists()){
            cacheDir.deleteRecursively()
        }

        // Delete book content cache
        context.cacheDir.listFiles { file ->
            file.name.startsWith("book_content_")
        }?.forEach { file ->
            file.delete()
        }
    }
}
