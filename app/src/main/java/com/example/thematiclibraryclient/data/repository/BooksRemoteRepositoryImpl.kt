package com.example.thematiclibraryclient.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.thematiclibraryclient.data.mapper.toConnectionExceptionDomainModel
import com.example.thematiclibraryclient.data.remote.api.IBooksApi
import com.example.thematiclibraryclient.data.remote.model.books.toDomainModel
import com.example.thematiclibraryclient.domain.common.TResult
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.domain.model.common.ConnectionExceptionDomainModel
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class BooksRemoteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val booksApi: IBooksApi
) : IBooksRemoteRepository {

    override suspend fun getBooks(): TResult<List<BookDomainModel>, ConnectionExceptionDomainModel> {
        return try {
            val booksApiModel = booksApi.getBooks()
            TResult.Success(booksApiModel.map { it.toDomainModel() })
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun getBookContent(bookId: Int): TResult<String, ConnectionExceptionDomainModel> {
        return try {
            val responseBody = booksApi.getBookContent(bookId)
            TResult.Success(responseBody.string())
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun getBookDetails(bookId: Int): TResult<BookDetailsDomainModel, ConnectionExceptionDomainModel> {
        return try {
            val details = booksApi.getBookDetails(bookId)
            TResult.Success(details.toDomainModel())
        } catch (e: Throwable) {
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }

    override suspend fun uploadBook(fileUri: Uri): TResult<BookDomainModel, ConnectionExceptionDomainModel> {
        return try {
            val contentResolver = context.contentResolver

            var filename = "unknown_file"

            contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                if(cursor.moveToFirst()){
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if(nameIndex != -1){
                        filename = cursor.getString(nameIndex)
                    }
                }
            }

            val inputStream = contentResolver.openInputStream(fileUri) ?:
            return TResult.Error(Exception("Cannot open input stream").toConnectionExceptionDomainModel())

            val requestBody = inputStream.readBytes().toRequestBody(
                contentResolver.getType(fileUri)?.toMediaTypeOrNull()
            )

            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                filename,
                requestBody
            )

            val uploadedBook = booksApi.uploadBook(multipartBody)

            TResult.Success(uploadedBook.toDomainModel())

        } catch (e : Exception){
            TResult.Error(e.toConnectionExceptionDomainModel())
        }
    }
}