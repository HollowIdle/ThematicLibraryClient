package com.example.thematiclibraryclient.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.entity.BookEntity
import com.example.thematiclibraryclient.data.local.entity.toDetailsDomainModel
import com.example.thematiclibraryclient.data.local.entity.toDomainModel
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBooksApi
import com.example.thematiclibraryclient.data.remote.model.books.BookProgressRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.books.UpdateDescriptionRequestApiModel
import com.example.thematiclibraryclient.data.remote.model.books.toDomainModel
import com.example.thematiclibraryclient.data.remote.model.common.StringListRequestApiModel
import com.example.thematiclibraryclient.domain.common.LocalBookParser
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
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
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import javax.inject.Inject

class BooksRemoteRepositoryImpl @Inject constructor(
    private val booksApi: IBooksApi,
    private val booksDao: BooksDao,
    private val localBookParser: LocalBookParser,
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

            val entities = apiBooks.map { apiBook ->
                val existing = booksDao.getBookByServerId(apiBook.id)

                BookEntity(
                    id = existing?.id ?: 0,
                    serverId = apiBook.id,
                    title = apiBook.title,
                    description = apiBook.description,
                    authors = apiBook.authors.map { it.toDomainModel() },
                    tags = apiBook.tags,
                    shelfIds = apiBook.shelfIds,
                    filePath = existing?.filePath,
                    isSynced = true,
                    isDetailsLoaded = false
                )
            }
            booksDao.insertBooks(entities)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override fun getBookDetails(bookId: Int): Flow<BookDetailsDomainModel?> {
        return booksDao.getBookById(bookId).map { entity ->
            entity?.toDetailsDomainModel()
        }
    }

    override suspend fun refreshBookDetails(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        val localBook = booksDao.getBookEntityById(bookId)
        if (localBook?.serverId == null) return TResult.Success(Unit)

        return try {
            val apiDetails = booksApi.getBookDetails(localBook.serverId)

            val updatedEntity = localBook.copy(
                title = apiDetails.title,
                description = apiDetails.description,
                authors = apiDetails.authors.map { AuthorDomainModel(it) },
                tags = apiDetails.tags,
                shelfIds = apiDetails.shelfIds,
                lastPosition = apiDetails.lastPosition ?: localBook.lastPosition,
                isDetailsLoaded = true,
                isSynced = true
            )

            booksDao.insertBook(updatedEntity)
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
                val bookEntity = booksDao.getBookEntityById(bookId)
                    ?: return@withContext TResult.Error(Exception("Book not found in DB").toConnectionExceptionDomainModel())

                val booksDir = File(context.filesDir, "books")
                if (!booksDir.exists()) booksDir.mkdirs()

                val currentFilePath = bookEntity.filePath
                if (!currentFilePath.isNullOrEmpty()) {
                    val localFile = File(booksDir, currentFilePath)
                    if (localFile.exists() && localFile.length() > 0) {
                        return@withContext TResult.Success(localFile)
                    }
                }

                if (bookEntity.serverId != null) {
                    val response = booksApi.downloadBook(bookEntity.serverId)
                    if (!response.isSuccessful || response.body() == null) {
                        throw Exception("Ошибка загрузки: ${response.code()}")
                    }

                    val responseBody = response.body()!!
                    val finalFileName = getFileNameFromResponse(response, bookEntity.serverId)
                    val file = File(booksDir, finalFileName)

                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    val updatedBook = bookEntity.copy(filePath = finalFileName)
                    booksDao.insertBook(updatedBook)

                    return@withContext TResult.Success(file)
                } else {
                    throw Exception("Файл отсутствует локально и не привязан к серверу")
                }
            } catch (e: Throwable) {
                TResult.Error(e.toConnectionExceptionDomainModel())
            }
        }
    }

    override suspend fun addLocalBook(fileUri: Uri): TResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver

                var fileName = "unknown_book"
                contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    }
                }

                val booksDir = File(context.filesDir, "books")
                if (!booksDir.exists()) booksDir.mkdirs()

                val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
                val destFile = File(booksDir, uniqueFileName)

                contentResolver.openInputStream(fileUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext TResult.Error(Exception("Cannot open input stream"))

                val metadata = localBookParser.parseMetadata(destFile)

                val newBook = BookEntity(
                    title = metadata.title,
                    description = metadata.description,
                    authors = metadata.authors.map { AuthorDomainModel(it) },
                    tags = emptyList(),
                    shelfIds = emptyList(),
                    filePath = uniqueFileName,
                    isSynced = false,
                    serverId = null,
                    isDetailsLoaded = true
                )

                booksDao.insertBook(newBook)

                TResult.Success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                TResult.Error(e)
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

    // Заглушка
    override suspend fun uploadBook(fileUri: Uri): TResult<Unit, ConnectionExceptionDomainModel> {
        return TResult.Success(Unit)
    }

    override suspend fun deleteBook(bookId: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        return try {
            val book = booksDao.getBookEntityById(bookId)
            if (book != null) {

                book.filePath?.let { path ->
                    if (path.isNotEmpty()) {
                        val file = File(context.filesDir, "books/$path")
                        if (file.exists()) file.delete()
                    }
                }

                if (book.serverId == null) {
                    booksDao.deleteBookPhysically(bookId)
                } else {
                    booksDao.markAsDeleted(bookId)

                    try {
                        booksApi.deleteBook(book.serverId)
                        booksDao.deleteBookPhysically(bookId)
                    } catch (e: Exception) {
                    }
                }
            }
            _booksUpdateFlow.emit(Unit)
            TResult.Success(Unit)
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun updateProgress(bookId: Int, position: Int): TResult<Unit, ConnectionExceptionDomainModel> {
        booksDao.updateProgress(bookId, position)

        val book = booksDao.getBookEntityById(bookId)
        if (book?.serverId != null) {
            try {
                booksApi.updateProgress(book.serverId, BookProgressRequestApiModel(position))
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    override suspend fun updateAuthors(bookId: Int, authors: List<String>): TResult<Unit, ConnectionExceptionDomainModel> {
        val localBook = booksDao.getBookEntityById(bookId)
            ?: return TResult.Error(Exception("Book not found").toConnectionExceptionDomainModel())

        val newAuthors = authors.map { AuthorDomainModel(it) }
        val updatedBook = localBook.copy(
            authors = newAuthors,
            isSynced = false
        )
        booksDao.insertBook(updatedBook)

        if (localBook.serverId != null) {
            try {
                booksApi.updateAuthors(localBook.serverId, StringListRequestApiModel(authors))
                booksDao.insertBook(updatedBook.copy(isSynced = true))
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    override suspend fun updateTags(bookId: Int, tags: List<String>): TResult<Unit, ConnectionExceptionDomainModel> {
        val localBook = booksDao.getBookEntityById(bookId)
            ?: return TResult.Error(Exception("Book not found").toConnectionExceptionDomainModel())

        val updatedBook = localBook.copy(
            tags = tags,
            isSynced = false
        )
        booksDao.insertBook(updatedBook)

        if (localBook.serverId != null) {
            try {
                booksApi.updateTags(localBook.serverId, StringListRequestApiModel(tags))
                booksDao.insertBook(updatedBook.copy(isSynced = true))
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }

    override suspend fun updateDescription(bookId: Int, description: String): TResult<Unit, ConnectionExceptionDomainModel> {
        val localBook = booksDao.getBookEntityById(bookId)
            ?: return TResult.Error(Exception("Book not found").toConnectionExceptionDomainModel())

        val updatedBook = localBook.copy(
            description = description,
            isSynced = false
        )
        booksDao.insertBook(updatedBook)

        if (localBook.serverId != null) {
            try {
                booksApi.updateDescription(localBook.serverId, UpdateDescriptionRequestApiModel(description))
                booksDao.insertBook(updatedBook.copy(isSynced = true))
            } catch (e: Exception) {
            }
        }
        return TResult.Success(Unit)
    }
}