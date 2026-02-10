package com.example.thematiclibraryclient.data.local.source

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaginationCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private fun getCacheDir(): File {
        val dir = File(context.cacheDir, "pagination_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getCacheFile(key: String): File {
        val fileName = "pg_${key.hashCode()}.json"
        return File(getCacheDir(), fileName)
    }

    suspend fun saveOffsets(key: String, offsets: List<Int>) = withContext(Dispatchers.IO) {
        try {
            val file = getCacheFile(key)
            val json = gson.toJson(offsets)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getOffsets(key: String): List<Int>? = withContext(Dispatchers.IO) {
        try {
            val file = getCacheFile(key)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<Int>>() {}.type
                return@withContext gson.fromJson<List<Int>>(json, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}