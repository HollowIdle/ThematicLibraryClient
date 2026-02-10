package com.example.thematiclibraryclient.domain.common

import android.util.Log
import com.example.thematiclibraryclient.domain.model.books.LocalBookMetadata
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.zwobble.mammoth.DocumentConverter
import org.zwobble.mammoth.Result
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBookParser @Inject constructor() {

    fun parseMetadata(file: File): LocalBookMetadata {
        val extension = file.extension.lowercase()
        val metadata = try {
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

        return metadata
    }

    private fun parseEpubMetadata(file: File): LocalBookMetadata {
        val unzipDir = File(file.parent, "${file.name}_meta_temp")
        if (unzipDir.exists()) unzipDir.deleteRecursively()

        try {
            unzip(file, unzipDir)

            val containerXml = File(unzipDir, "META-INF/container.xml")
            if (!containerXml.exists()) throw Exception("No container.xml")

            val containerDoc = Jsoup.parse(containerXml.inputStream(), "UTF-8", "", Parser.xmlParser())
            val rootPath = containerDoc.getElementsByTag("rootfile").attr("full-path")
            val opfFile = File(unzipDir, rootPath)

            if (!opfFile.exists()) throw Exception("No OPF file")

            val opfDoc = Jsoup.parse(opfFile.inputStream(), "UTF-8", "", Parser.xmlParser())

            val title = opfDoc.select("dc|title, title").text().ifBlank { file.nameWithoutExtension }

            val authors = opfDoc.select("dc|creator, creator").map { it.text() }
            val description = opfDoc.select("dc|description, description").text()

            return LocalBookMetadata(title, authors, description.ifBlank { null })
        } finally {
            unzipDir.deleteRecursively()
        }
    }

    private fun parseFb2Metadata(file: File): LocalBookMetadata {
        val charset = detectFb2Encoding(file)

        val doc = Jsoup.parse(file.inputStream(), charset, "", Parser.xmlParser())

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

    private fun detectFb2Encoding(file: File): String {
        try {
            file.inputStream().use { fis ->
                val buffer = ByteArray(1024)
                val read = fis.read(buffer)
                if (read > 0) {
                    val header = String(buffer, 0, read)
                    val pattern = Pattern.compile("encoding=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(header)
                    if (matcher.find()) {
                        return matcher.group(1) ?: "UTF-8"
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LocalBookParser", "Failed to detect encoding, defaulting to UTF-8")
        }
        return "UTF-8"
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


    private fun parseFb2(file: File): String {
        val charset = detectFb2Encoding(file)
        val doc = Jsoup.parse(file.inputStream(), charset, "", Parser.xmlParser())

        val body = doc.select("body").first() ?: return "Ошибка: Пустой файл"

        body.select("binary").remove()
        body.select("image").remove()

        return body.html()
    }

    private fun parseEpub(file: File): String {
        val unzipDir = File(file.parent, "${file.name}_unzipped")
        if (unzipDir.exists()) unzipDir.deleteRecursively()

        unzip(file, unzipDir)

        try {
            val containerXml = File(unzipDir, "META-INF/container.xml")
            if (!containerXml.exists()) return "Ошибка: Некорректный EPUB (нет container.xml)"

            val containerDoc = Jsoup.parse(containerXml.inputStream(), "UTF-8", "", Parser.xmlParser())
            val rootPath = containerDoc.getElementsByTag("rootfile").attr("full-path")
            if (rootPath.isEmpty()) return "Ошибка: Некорректный EPUB (не найден rootfile)"

            val opfFile = File(unzipDir, URLDecoder.decode(rootPath, "UTF-8"))
            if (!opfFile.exists()) return "Ошибка: Некорректный EPUB (не найден .opf файл)"

            val opfDir = opfFile.parentFile ?: unzipDir
            val opfDoc = Jsoup.parse(opfFile.inputStream(), "UTF-8", "", Parser.xmlParser())

            val manifest = opfDoc.select("manifest item").associate {
                it.attr("id") to Pair(it.attr("href"), it.attr("media-type"))
            }

            val spine = opfDoc.select("spine itemref").mapNotNull { it.attr("idref") }

            val contentHtml = StringBuilder("<html><head><style>img { max-width: 100%; height: auto; }</style></head><body>")

            for (idRef in spine) {
                manifest[idRef]?.let { (href, mediaType) ->
                    val decodedHref = URLDecoder.decode(href, "UTF-8")
                    val itemFile = File(opfDir, decodedHref)

                    if (itemFile.exists()) {
                        when {
                            mediaType.contains("xhtml") || mediaType.contains("html") -> {
                                val doc = Jsoup.parse(itemFile.readText(Charset.forName("UTF-8")))
                                doc.head().remove()
                                doc.body().select("img").remove()

                                doc.body().select("svg image").remove()
                                contentHtml.append(doc.body().html())
                            }
                        }
                    }
                }
            }

            contentHtml.append("</body></html>")
            return contentHtml.toString()
        } catch (e: Exception) {
            Log.e("LocalBookParser", "Error parsing EPUB content for ${file.name}", e)
            return "<html><body><h1>Ошибка при разборе файла</h1><p>${e.message}</p></body></html>"
        } finally {
            unzipDir.deleteRecursively()
        }
    }

    private fun getMediaTypeFromExtension(extension: String): String {
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }


    private fun parseDocx(file: File): String {
        val converter = DocumentConverter()
        val result: Result<String> = converter.convertToHtml(file)
        return "<html><body>${result.value}</body></html>"
    }

    private fun parseTxt(file: File): String {
        val text = file.readText(Charset.defaultCharset())
        val paragraphs = text.split("\n").joinToString("") { "<p>$it</p>" }
        return "<html><body>$paragraphs</body></html>"
    }

    private fun unzip(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip Path Traversal Vulnerability detected")
                }

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
