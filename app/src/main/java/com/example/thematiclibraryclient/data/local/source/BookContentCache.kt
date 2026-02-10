package com.example.thematiclibraryclient.data.local.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getCacheFile(bookId: Int): File {
        return File(context.cacheDir, "book_content_$bookId.html")
    }

    suspend fun saveContent(bookId: Int, content: String) = withContext(Dispatchers.IO) {
        try {
            getCacheFile(bookId).writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadContent(bookId: Int): String? = withContext(Dispatchers.IO) {
        val file = getCacheFile(bookId)
        if (file.exists() && file.length() > 0) {
            try {
                return@withContext file.readText()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext null
    }

    suspend fun clearCache(bookId: Int) = withContext(Dispatchers.IO) {
        val file = getCacheFile(bookId)
        if (file.exists()) file.delete()
    }
}
