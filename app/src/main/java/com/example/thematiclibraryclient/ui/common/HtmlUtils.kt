package com.example.thematiclibraryclient.ui.common

import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun parseHtmlToAnnotatedString(html: String): AnnotatedString = withContext(Dispatchers.Default) {
    if (html.length < 50_000) {
        return@withContext convertChunk(html)
    }

    val builder = AnnotatedString.Builder()
    var currentIndex = 0
    val chunkSize = 10_000

    while (currentIndex < html.length) {
        var endIndex = currentIndex + chunkSize
        if (endIndex >= html.length) {
            endIndex = html.length
        } else {
            val pEnd = html.indexOf("</p>", endIndex)
            val brEnd = html.indexOf("<br>", endIndex)

            val safeEnd = if (pEnd != -1 && pEnd - endIndex < 5000) pEnd + 4
            else if (brEnd != -1 && brEnd - endIndex < 5000) brEnd + 4
            else endIndex
            endIndex = safeEnd
        }

        val chunk = html.substring(currentIndex, endIndex)
        if (chunk.isNotBlank()) {
            val annotatedChunk = convertChunk(chunk)
            builder.append(annotatedChunk)
        }
        currentIndex = endIndex
    }

    return@withContext builder.toAnnotatedString()
}

private fun convertChunk(htmlChunk: String): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(htmlChunk, HtmlCompat.FROM_HTML_MODE_COMPACT)

    return buildAnnotatedString {
        append(spanned.toString())

        val styleSpans = spanned.getSpans(0, spanned.length, StyleSpan::class.java)
        val underlineSpans = spanned.getSpans(0, spanned.length, UnderlineSpan::class.java)

        styleSpans.forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            when (span.style) {
                android.graphics.Typeface.BOLD -> {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                }
                android.graphics.Typeface.ITALIC -> {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                }
                android.graphics.Typeface.BOLD_ITALIC -> {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                }
            }
        }

        underlineSpans.forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
        }
    }
}