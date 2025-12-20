package com.example.thematiclibraryclient.domain.common

import android.util.Log
import com.example.thematiclibraryclient.domain.model.books.LocalBookMetadata
import org.jsoup.Jsoup
import org.zwobble.mammoth.DocumentConverter
import org.zwobble.mammoth.Result
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBookParser @Inject constructor() {

    fun parseMetadata(file: File): LocalBookMetadata {
        val extension = file.extension.lowercase()
        return try {
            when (extension) {
                "epub" -> parseEpubMetadata(file)
                "fb2" -> parseFb2Metadata(file)
                else -> LocalBookMetadata(
                    title = file.nameWithoutExtension,
                    authors = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e("LocalBookParser", "Error parsing metadata for ${file.name}", e)
            LocalBookMetadata(title = file.nameWithoutExtension, authors = emptyList())
        }
    }

    private fun parseEpubMetadata(file: File): LocalBookMetadata {
        val unzipDir = File(file.parent, "${file.name}_meta_temp")
        if (unzipDir.exists()) unzipDir.deleteRecursively()

        try {

            unzip(file, unzipDir)

            val containerXml = File(unzipDir, "META-INF/container.xml")
            if (!containerXml.exists()) throw Exception("No container.xml")

            val containerDoc = Jsoup.parse(containerXml, "UTF-8")
            val rootPath = containerDoc.getElementsByTag("rootfile").attr("full-path")
            val opfFile = File(unzipDir, rootPath)

            if (!opfFile.exists()) throw Exception("No OPF file")

            val opfDoc = Jsoup.parse(opfFile, "UTF-8")

            val title = opfDoc.getElementsByTag("dc:title").text().ifBlank { file.nameWithoutExtension }
            val authors = opfDoc.getElementsByTag("dc:creator").map { it.text() }
            val description = opfDoc.getElementsByTag("dc:description").text()

            return LocalBookMetadata(title, authors, description.ifBlank { null })
        } finally {
            unzipDir.deleteRecursively()
        }
    }

    private fun parseFb2Metadata(file: File): LocalBookMetadata {
        val doc = Jsoup.parse(file, "UTF-8")
        val titleInfo = doc.select("description title-info")

        val title = titleInfo.select("book-title").text().ifBlank { file.nameWithoutExtension }

        val authors = titleInfo.select("author").map { authorTag ->
            val first = authorTag.select("first-name").text()
            val last = authorTag.select("last-name").text()
            val middle = authorTag.select("middle-name").text()
            listOf(first, middle, last).filter { it.isNotBlank() }.joinToString(" ")
        }.filter { it.isNotBlank() }

        val description = titleInfo.select("annotation").text()

        return LocalBookMetadata(title, authors, description.ifBlank { null })
    }

    fun parseToHtml(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "epub" -> parseEpub(file)
            "fb2" -> parseFb2(file)
            "txt" -> parseTxt(file)
            "docx" -> parseDocx(file)
            else -> throw IllegalArgumentException("Unsupported text format: $extension")
        }
    }

    private fun parseDocx(file: File): String {
        val converter = DocumentConverter()
        val result: Result<String> = converter.convertToHtml(file)
        val html = result.value

        return "<html><body>$html</body></html>"
    }

    private fun parseTxt(file: File): String {
        val text = file.readText()
        val paragraphs = text.split("\n").joinToString("") { "<p>$it</p>" }
        return "<html><body>$paragraphs</body></html>"
    }

    private fun parseFb2(file: File): String {
        val charsetName = detectXmlEncoding(file) ?: "UTF-8"

        val doc = Jsoup.parse(file, charsetName, "", org.jsoup.parser.Parser.xmlParser())

        val body = doc.select("body").first() ?: return "Ошибка: Пустой файл"

        body.select("image").remove()

        return body.html()
    }

    private fun detectXmlEncoding(file: File): String? {
        try {
            file.inputStream().use { fis ->
                val reader = java.io.BufferedReader(java.io.InputStreamReader(fis))
                val firstLine = reader.readLine()

                if (firstLine != null && firstLine.contains("encoding=")) {
                    val regex = "encoding=['\"]([^'\"]+)['\"]".toRegex()
                    val match = regex.find(firstLine)
                    return match?.groupValues?.get(1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseEpub(file: File): String {
        val unzipDir = File(file.parent, "${file.name}_unzipped")
        if (unzipDir.exists()) unzipDir.deleteRecursively()

        unzip(file, unzipDir)

        try {
            val containerXml = File(unzipDir, "META-INF/container.xml")
            if (!containerXml.exists()) {
                return "Ошибка: Некорректный EPUB (нет container.xml)"
            }

            val containerDoc = Jsoup.parse(containerXml, "UTF-8")

            val rootFileTag = containerDoc.getElementsByTag("rootfile").first()
            val opfPathRaw = rootFileTag?.attr("full-path")

            if (opfPathRaw.isNullOrEmpty()) {
                return "Ошибка: Не удалось найти структуру книги"
            }

            val opfFile = File(unzipDir, opfPathRaw)
            if (!opfFile.exists()) {
                return "Ошибка: Файл структуры отсутствует"
            }

            val opfDir = opfFile.parentFile

            val opfDoc = Jsoup.parse(opfFile, "UTF-8")

            val manifest = mutableMapOf<String, String>()
            opfDoc.getElementsByTag("item").forEach { item ->
                val id = item.attr("id")
                val href = item.attr("href")
                manifest[id] = URLDecoder.decode(href, "UTF-8")
            }

            val spineRefs = opfDoc.getElementsByTag("itemref").map { it.attr("idref") }

            if (spineRefs.isEmpty()) {
                return "Ошибка: Пустое содержание книги"
            }

            val fullHtml = StringBuilder("<html><body>")
            var chaptersFound = 0

            for (idRef in spineRefs) {
                val href = manifest[idRef]
                if (href == null) {
                    continue
                }

                val chapterFile = File(opfDir, href)
                if (chapterFile.exists()) {
                    val chapterDoc = Jsoup.parse(chapterFile, "UTF-8")

                    val body = chapterDoc.getElementsByTag("body").first()
                    if (body != null) {
                        fullHtml.append(body.html())
                        fullHtml.append("<br/><br/>")
                        chaptersFound++
                    }
                }
            }

            fullHtml.append("</body></html>")


            if (chaptersFound == 0) return "Ошибка: Главы не найдены"

            return fullHtml.toString()

        } catch (e: Exception) {
            throw e
        } finally {
            try {
                unzipDir.deleteRecursively()
            } catch (e: Exception) {
            }
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    newFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}