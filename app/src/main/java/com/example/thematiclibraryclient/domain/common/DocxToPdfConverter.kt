package com.example.thematiclibraryclient.domain.common

import android.content.Context
import android.util.Log
import com.example.thematiclibraryclient.domain.common.DocxToHtmlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mddanishansari.html_to_pdf.HtmlToPdfConvertor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DocxToPdfConverter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val docxParser: DocxToHtmlParser
) {

    suspend fun convert(docxFile: File): File {
        val htmlString = withContext(Dispatchers.Default) {
            docxParser.parse(docxFile)
        }
        Log.d("Pdf", htmlString)
        val booksDir = File(context.filesDir, "books")
        if (!booksDir.exists()) booksDir.mkdirs()

        val finalPdfFile = File(booksDir, "${docxFile.nameWithoutExtension}_converted.pdf")

        val tempPdfFile = File(booksDir, "${docxFile.nameWithoutExtension}_temp.pdf")

        if (finalPdfFile.exists() && finalPdfFile.length() > 1024) {
            return finalPdfFile
        }

        if (tempPdfFile.exists()) {
            tempPdfFile.delete()
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val convertor = HtmlToPdfConvertor(context)

                try {
                    convertor.convert(
                        pdfLocation = tempPdfFile,
                        htmlString = htmlString,
                        onPdfGenerationFailed = { exception ->
                            if (tempPdfFile.exists()) tempPdfFile.delete()

                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        },
                        onPdfGenerated = { generatedFile ->
                            if (generatedFile.renameTo(finalPdfFile)) {
                                if (continuation.isActive) {
                                    continuation.resume(finalPdfFile)
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(generatedFile)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    if (tempPdfFile.exists()) tempPdfFile.delete()
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                continuation.invokeOnCancellation {
                    try {
                        if (tempPdfFile.exists()) tempPdfFile.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}