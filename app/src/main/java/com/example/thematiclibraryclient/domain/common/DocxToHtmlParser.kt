package com.example.thematiclibraryclient.domain.common

import android.util.Base64
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

class DocxToHtmlParser @Inject constructor() {

    private val imagesMap = mutableMapOf<String, ByteArray>()
    private val relationsMap = mutableMapOf<String, String>()

    private val numIdToAbstractNumId = mutableMapOf<String, String>()
    private val abstractNumToFormat = mutableMapOf<String, MutableMap<String, String>>()

    private val listCounters = mutableMapOf<String, Int>()

    private val listStack = mutableListOf<ListContext>()

    data class ListContext(val type: String, val numId: String, val ilvl: String)

    private fun getWAttribute(parser: XmlPullParser, name: String): String? {
        return parser.getAttributeValue(null, "w:$name")
            ?: parser.getAttributeValue(null, name)
    }

    fun parse(file: File): String {
        Log.d("DocxParser", "Start parsing: ${file.name}")

        clearState()

        var documentXmlContent: String? = null

        try {
            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val name = entry!!.name
                        if (name.contains("__MACOSX")) continue

                        when {
                            name.startsWith("word/media/") -> {
                                imagesMap[File(name).name] = zis.readBytes()
                            }
                            name.endsWith("word/_rels/document.xml.rels") -> {
                                parseRelations(zis)
                            }
                            name.endsWith("word/numbering.xml") -> {
                                parseNumbering(zis.readBytes())
                            }
                            name.endsWith("word/document.xml") -> {
                                documentXmlContent = String(zis.readBytes(), Charsets.UTF_8)
                            }
                        }
                    }
                }
            }

            if (documentXmlContent == null) return errorHtml("document.xml not found")

            val sb = StringBuilder()
            sb.append("<html><head><style>")
            sb.append("body { font-family: 'Times New Roman', serif; padding: 30px; line-height: 1.5; color: #000; font-size: 14px; }")
            sb.append("p { margin: 0 0 10px 0; text-align: justify; }")
            sb.append("h1 { font-size: 22px; font-weight: bold; margin-top: 20px; }")
            sb.append("h2 { font-size: 18px; font-weight: bold; margin-top: 15px; }")
            sb.append("table { border-collapse: collapse; width: 100%; margin-bottom: 15px; }")
            sb.append("td, th { border: 1px solid #000; padding: 5px; vertical-align: top; }")
            sb.append("img { max-width: 100%; height: auto; display: block; margin: 10px auto; }")
            sb.append("ul, ol { margin-top: 0; margin-bottom: 10px; padding-left: 40px; }")
            sb.append("li { margin-bottom: 5px; }")
            sb.append("a { color: blue; text-decoration: underline; }")
            sb.append("</style></head><body>")

            sb.append(parseDocumentXml(documentXmlContent!!))

            sb.append("</body></html>")
            return sb.toString()

        } catch (e: Exception) {
            Log.e("DocxParser", "Zip error", e)
            return errorHtml(e.message)
        } finally {
            clearState()
            documentXmlContent = null
        }
    }

    private fun parseNumbering(content: ByteArray) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(content), "UTF-8")

        var eventType = parser.eventType
        var currentAbstractNumId: String? = null
        var currentLvl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = parser.name ?: ""
                if (isTag(name, "abstractNum")) {
                    currentAbstractNumId = getWAttribute(parser, "abstractNumId")
                } else if (isTag(name, "lvl")) {
                    currentLvl = getWAttribute(parser, "ilvl")
                } else if (isTag(name, "numFmt") && currentAbstractNumId != null && currentLvl != null) {
                    val fmt = getWAttribute(parser, "val")
                    if (fmt != null) {
                        abstractNumToFormat.getOrPut(currentAbstractNumId!!) { mutableMapOf() }[currentLvl!!] = fmt
                    }
                } else if (isTag(name, "num")) {
                    val numId = getWAttribute(parser, "numId")
                    if (numId != null) {
                        val depth = parser.depth
                        while (parser.next() != XmlPullParser.END_DOCUMENT) {
                            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) break
                            if (parser.eventType == XmlPullParser.START_TAG && isTag(parser.name, "abstractNumId")) {
                                val absId = getWAttribute(parser, "val")
                                if (absId != null) numIdToAbstractNumId[numId] = absId
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseDocumentXml(xmlContent: String): String {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xmlContent.toByteArray()), "UTF-8")

        val html = StringBuilder()
        var eventType = parser.eventType

        var isParagraphOpen = false
        var currentTag = "p"
        var currentPStyle = ""
        var currentHyperlinkUrl: String? = null

        var isBold = false; var isItalic = false; var isUnderline = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        isTag(name, "p") -> {
                            isParagraphOpen = false
                            currentTag = "p"
                            currentPStyle = ""
                        }

                        isTag(name, "pPr") -> {
                            val props = parseParagraphProperties(parser)
                            currentPStyle = props.css

                            if (props.styleVal != null) {
                                when (props.styleVal) {
                                    "Heading1", "1" -> currentTag = "h1"
                                    "Heading2", "2" -> currentTag = "h2"
                                    "Heading3", "3" -> currentTag = "h3"
                                }
                            }

                            if (props.numId != null && props.numId != "0") {
                                val level = props.ilvl?.toIntOrNull() ?: 0
                                val format = getListFormat(props.numId, level.toString())

                                syncListStack(html, level, format, props.numId)

                                val counterKey = "${props.numId}:$level"
                                val currentCount = listCounters.getOrDefault(counterKey, 0)
                                listCounters[counterKey] = currentCount + 1

                                currentTag = "li"
                            } else {
                                closeAllLists(html)
                            }
                        }

                        isTag(name, "r") -> {
                            if (!isParagraphOpen) {
                                html.append("<$currentTag")
                                if (currentPStyle.isNotEmpty()) html.append(" style=\"$currentPStyle\"")
                                html.append(">")
                                isParagraphOpen = true
                            }
                            if (currentHyperlinkUrl != null) html.append("<a href=\"$currentHyperlinkUrl\">")
                        }

                        isTag(name, "b") -> isBold = true
                        isTag(name, "i") -> isItalic = true
                        isTag(name, "u") -> isUnderline = true
                        isTag(name, "br") -> html.append("<br/>")
                        isTag(name, "color") -> {
                            val color = getWAttribute(parser, "val")
                            if (color != null && color != "auto") html.append("<span style=\"color:#$color\">")
                        }
                        isTag(name, "sz") -> {
                            val size = getWAttribute(parser, "val")?.toIntOrNull()
                            if (size != null) html.append("<span style=\"font-size:${size/2}pt\">")
                        }

                        isTag(name, "t") -> {
                            try {
                                val text = parser.nextText()
                                if (text.isNotEmpty()) {
                                    if (isBold) html.append("<b>")
                                    if (isItalic) html.append("<i>")
                                    if (isUnderline) html.append("<u>")
                                    html.append(text.replace("&", "&amp;").replace("<", "&lt;"))
                                    if (isUnderline) html.append("</u>")
                                    if (isItalic) html.append("</i>")
                                    if (isBold) html.append("</b>")
                                }
                            } catch (e: Exception) {}
                        }

                        isTag(name, "hyperlink") -> {
                            val rId = findAttribute(parser, "id")
                            currentHyperlinkUrl = relationsMap[rId]
                        }

                        isTag(name, "tbl") -> {
                            closeAllLists(html)
                            html.append("<table>")
                        }
                        isTag(name, "tr") -> html.append("<tr>")
                        isTag(name, "tc") -> html.append("<td>")

                        isTag(name, "blip") || isTag(name, "imagedata") -> {
                            if (!isParagraphOpen && currentTag != "li") {
                                html.append("<p style=\"text-align:center\">")
                                isParagraphOpen = true
                            }
                            val rId = findAttribute(parser, "embed") ?: findAttribute(parser, "id")
                            if (rId != null) {
                                html.append(generateImgTag(rId))
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name ?: ""
                    when {
                        isTag(name, "p") -> {
                            if (isParagraphOpen) {
                                html.append("</$currentTag>")
                                isParagraphOpen = false
                            } else {
                                html.append("<$currentTag>&nbsp;</$currentTag>")
                            }
                        }
                        isTag(name, "r") -> {
                            html.append("</span></span>")
                            if (currentHyperlinkUrl != null) html.append("</a>")
                            isBold = false; isItalic = false; isUnderline = false
                        }
                        isTag(name, "hyperlink") -> currentHyperlinkUrl = null
                        isTag(name, "tbl") -> html.append("</table>")
                        isTag(name, "tr") -> html.append("</tr>")
                        isTag(name, "tc") -> html.append("</td>")
                    }
                }
            }
            eventType = parser.next()
        }

        closeAllLists(html)
        return html.toString()
    }

    private fun syncListStack(html: StringBuilder, targetLevel: Int, format: String, numId: String) {
        while (listStack.size > targetLevel + 1) {
            val context = listStack.removeAt(listStack.lastIndex)
            html.append("</${context.type}>")
        }

        if (listStack.size == targetLevel + 1) {
            val current = listStack.last()
            if (current.numId != numId) {
                listStack.removeAt(listStack.lastIndex)
                html.append("</${current.type}>")
            }
        }

        while (listStack.size <= targetLevel) {
            val tagType = if (format == "bullet") "ul" else "ol"
            val cssStyle = getCssListStyle(format)

            val counterKey = "$numId:${listStack.size}"
            val startVal = listCounters.getOrDefault(counterKey, 0) + 1

            val startAttr = if (tagType == "ol" && startVal > 1) " start=\"$startVal\"" else ""

            html.append("<$tagType style=\"list-style-type: $cssStyle\"$startAttr>")
            listStack.add(ListContext(tagType, numId, listStack.size.toString()))
        }
    }

    private fun closeAllLists(html: StringBuilder) {
        while (listStack.isNotEmpty()) {
            val context = listStack.removeAt(listStack.lastIndex)
            html.append("</${context.type}>")
        }
    }

    private fun getCssListStyle(format: String): String {
        return when (format) {
            "bullet" -> "disc"
            "decimal" -> "decimal"
            "lowerLetter" -> "lower-alpha"
            "upperLetter" -> "upper-alpha"
            "lowerRoman" -> "lower-roman"
            "upperRoman" -> "upper-roman"
            else -> "decimal"
        }
    }
    private fun findAttribute(parser: XmlPullParser, attrName: String): String? {
        for (i in 0 until parser.attributeCount) {
            val name = parser.getAttributeName(i)
            if (name == attrName || name.endsWith(":$attrName")) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    data class ParagraphProps(val css: String, val numId: String?, val ilvl: String?, val styleVal: String?)

    private fun parseParagraphProperties(parser: XmlPullParser): ParagraphProps {
        val sb = StringBuilder()
        var numId: String? = null
        var ilvl: String? = null
        var styleVal: String? = null

        val startDepth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == startDepth) break

            if (parser.eventType == XmlPullParser.START_TAG) {
                val name = parser.name ?: ""

                if (isTag(name, "jc")) {
                    val align = getWAttribute(parser, "val")
                    if (align == "center") sb.append("text-align: center; ")
                    if (align == "right") sb.append("text-align: right; ")
                    if (align == "both") sb.append("text-align: justify; ")
                }
                if (isTag(name, "ind")) {
                    val left = getWAttribute(parser, "left")?.toIntOrNull()
                    val firstLine = getWAttribute(parser, "firstLine")?.toIntOrNull()
                    if (left != null) sb.append("margin-left: ${left / 15}px; ")
                    if (firstLine != null) sb.append("text-indent: ${firstLine / 15}px; ")
                }
                if (isTag(name, "numPr")) {
                    val numDepth = parser.depth
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.END_TAG && parser.depth == numDepth) break
                        if (parser.eventType == XmlPullParser.START_TAG) {
                            if (isTag(parser.name, "numId")) numId = getWAttribute(parser, "val")
                            if (isTag(parser.name, "ilvl")) ilvl = getWAttribute(parser, "val")
                        }
                    }
                }
                if (isTag(name, "pStyle")) {
                    styleVal = getWAttribute(parser, "val")
                }
            }
        }
        return ParagraphProps(sb.toString(), numId, ilvl, styleVal)
    }

    private fun getListFormat(numId: String, ilvl: String): String {
        val abstractId = numIdToAbstractNumId[numId] ?: return "decimal"
        return abstractNumToFormat[abstractId]?.get(ilvl) ?: "decimal"
    }

    private fun generateImgTag(rId: String): String {
        val fileName = relationsMap[rId] ?: return ""
        val cleanName = File(fileName).name
        val imageBytes = imagesMap[cleanName] ?: return ""
        val mimeType = if (cleanName.endsWith("png", true)) "image/png" else "image/jpeg"
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        return "<img src=\"data:$mimeType;base64,$base64\" />"
    }

    private fun isTag(name: String?, target: String): Boolean {
        if (name == null) return false
        return name.equals(target, true) || name.endsWith(":$target", true)
    }

    private fun parseSimpleXml(content: ByteArray, onTag: (XmlPullParser) -> Unit) {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(ByteArrayInputStream(content), "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) onTag(parser)
                eventType = parser.next()
            }
        } catch (e: Exception) {}
    }

    private fun parseRelations(inputStream: InputStream) {
        val content = inputStream.readBytes()
        parseSimpleXml(content) { parser ->
            if (parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) relationsMap[id] = target
            }
        }
    }

    private fun clearState() {
        imagesMap.clear()
        relationsMap.clear()
        numIdToAbstractNumId.clear()
        abstractNumToFormat.clear()
        listStack.clear()
        listCounters.clear()
    }

    private fun errorHtml(msg: String?) = "<html><body><h3>Error</h3><p>$msg</p></body></html>"
}