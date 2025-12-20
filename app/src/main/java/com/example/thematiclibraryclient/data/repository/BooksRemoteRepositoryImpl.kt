package com.example.thematiclibraryclient.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.entity.BookContentEntity
import com.example.thematiclibraryclient.data.local.entity.toDetailsDomainModel
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBooksApi
import com.example.thematiclibraryclient.data.remote.model.books.BookProgressRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.books.UpdateDescriptionRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.books.toEntity
import com.example.thematiclibraryclient.data.remote.model.common.StringListRequestApiModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import javax.inject.Inject

class BooksRemoteRepositoryImpl @Inject constructor(
    private val booksApi: IBooksApi,
    private val booksDao: BooksDao,
    @ApplicationContext private val context: Context
) : IBooksRemoteRepository {

    private val _booksUpdateFlow = MutableSharedFlow<Unit>(replay = 0)
    override val booksUpdateFlow = _booksUpdateFlow.asSharedFlow()

    override fun getBooks(): Flow<List<BookDomainModel>> {
        return booksDao.getBooks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun refreshBooks(): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiBooks = booksApi.getBooks()
            val entities = apiBooks.map { it.toEntity() }
            booksDao.insertBooks(entities)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            android.util.Log.e("BooksRepo", "CRITICAL ERROR saving books", e)
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override fun getBookDetails(bookId: Int): Flow<BookDetailsDomainModel?> {
        return booksDao.getBookById(bookId).map { entity ->
            entity?.toDetailsDomainModel()
        }
    }

    override suspend fun refreshBookDetails(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val apiDetails = booksApi.getBookDetails(bookId)
            booksDao.insertBook(apiDetails.toEntity())
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    /*override suspend fun getBookContent(bookId: Int): TResult<String, ConnectionExceptionDomainModel> {
        val cachedContent = booksDao.getBookContent(bookId)
        if (!cachedContent.isNullOrBlank()) {
            return TResult.Success(cachedContent)
        }

        return try {
            val responseBody = booksApi.getBookContent(bookId)
            val contentString = responseBody.string()

            booksDao.insertBookContent(BookContentEntity(bookId, contentString))

            TResult.Success(contentString)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }*/

    override suspend fun downloadBookFile(bookId: Int, fileName: String): TResult<File, ConnectionExceptionDomainModel> {
        return withContext(Dispatchers.IO) {
            try {
                val booksDir = File(context.filesDir, "books")
                if (!booksDir.exists()) booksDir.mkdirs()

                val existingFile = booksDir.listFiles()?.find { it.name.startsWith("${bookId}_") }
                if (existingFile != null && existingFile.length() > 0) {
                    return@withContext TResult.Success(existingFile)
                }

                val response = booksApi.downloadBook(bookId)

                if (!response.isSuccessful || response.body() == null) {
                    throw Exception("Ошибка загрузки: ${response.code()}")
                }

                val responseBody = response.body()!!

                val finalFileName = getFileNameFromResponse(response, bookId)

                val file = File(booksDir, finalFileName)

                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                TResult.Success(file)
            } catch (e: Throwable) {
                TResult.Error(e.toConnectionExceptionDomainModel())
            }
        }
    }

    private fun getFileNameFromResponse(response: Response<ResponseBody>, bookId: Int): String {
        val contentDisposition = response.headers()["Content-Disposition"]
        var extension = ""

        if (contentDisposition != null) {
            val pattern = Pattern.compile("filename=['\"]?([^'\";]+)['\"]?")
            val matcher = pattern.matcher(contentDisposition)
            if (matcher.find()) {
                val originalName = matcher.group(1)
                if (!originalName.isNullOrEmpty() && originalName.contains(".")) {
                    extension = originalName.substringAfterLast(".")
                }
            }
        }

        if (extension.isEmpty()) {
            val contentType = response.headers()["Content-Type"]
            if (contentType != null) {
                val mimeType = contentType.split(";")[0].trim()
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
            }
        }

        if (extension.isEmpty()) {
            extension = "bin"
        }

        return "${bookId}_book.$extension"
    }

    override suspend fun uploadBook(fileUri: Uri): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val contentResolver = context.contentResolver

            var fileName = "unknown_file"

            contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }

            val inputStream = contentResolver.openInputStream(fileUri)
                ?: return TResult.Error(Exception("Cannot open stream").toConnectionExceptionDomainModel())

            val requestBody = inputStream.readBytes().toRequestBody(
                contentResolver.getType(fileUri)?.toMediaTypeOrNull()
            )

            val multipartBody = MultipartBody.Part.createFormData("file", fileName, requestBody)

            booksApi.uploadBook(multipartBody)

            refreshBooks()

            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun deleteBook(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            booksApi.deleteBook(bookId)
            booksDao.deleteBook(bookId)
            _booksUpdateFlow.emit(Unit)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateProgress(bookId: Int, position: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        booksDao.updateProgress(bookId, position)

        return try {
            booksApi.updateProgress(bookId, BookProgressRequestApiModel(position))
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateAuthors(bookId: Int, authors: List<String>): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            booksApi.updateAuthors(bookId, StringListRequestApiModel(authors))
            refreshBookDetails(bookId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateTags(bookId: Int, tags: List<String>): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            booksApi.updateTags(bookId, StringListRequestApiModel(tags))
            refreshBookDetails(bookId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateDescription(bookId: Int, description: String): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            booksApi.updateDescription(bookId, UpdateDescriptionRequestApiModel(description))
            refreshBookDetails(bookId)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}