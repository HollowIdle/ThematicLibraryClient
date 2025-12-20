package com.example.thematiclibraryclient.ui.common

import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.min

@Composable
fun Paginator(
    fullText: AnnotatedString?,
    style: TextStyle,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPagesUpdated: (List<AnnotatedString>) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current

        val totalWidthPx = constraints.maxWidth
        val totalHeightPx = constraints.maxHeight

        val horizontalPaddingPx = with(density) {
            (contentPadding.calculateStartPadding(layoutDirection) +
                    contentPadding.calculateEndPadding(layoutDirection)).toPx().toInt()
        }
        val verticalPaddingPx = with(density) {
            (contentPadding.calculateTopPadding() +
                    contentPadding.calculateBottomPadding()).toPx().toInt()
        }

        val contentWidth = totalWidthPx - horizontalPaddingPx
        val contentHeight = totalHeightPx - verticalPaddingPx

        val textPaint = remember(style, density) {
            TextPaint().apply {
                isAntiAlias = true
                textSize = with(density) { style.fontSize.toPx() }
                color = android.graphics.Color.BLACK
                val typefaceStyle = when {
                    style.fontWeight == FontWeight.Bold && style.fontStyle == FontStyle.Italic -> Typeface.BOLD_ITALIC
                    style.fontWeight == FontWeight.Bold -> Typeface.BOLD
                    style.fontStyle == FontStyle.Italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)
            }
        }

        LaunchedEffect(fullText, contentWidth, contentHeight, style) {
            if (fullText != null && fullText.isNotEmpty() && contentWidth > 0 && contentHeight > 0) {
                paginateNative(
                    fullText = fullText,
                    textPaint = textPaint,
                    contentWidth = contentWidth,
                    contentHeight = contentHeight,
                    lineHeightMultiplier = 1.2f
                ).collect { updatedPages ->
                    onPagesUpdated(updatedPages)
                }
            }
        }
    }
}

private fun paginateNative(
    fullText: AnnotatedString,
    textPaint: TextPaint,
    contentWidth: Int,
    contentHeight: Int,
    lineHeightMultiplier: Float
): Flow<List<AnnotatedString>> = flow {
    val pages = mutableListOf<AnnotatedString>()
    var currentOffset = 0
    val totalLength = fullText.length

    emit(emptyList())

    while (currentOffset < totalLength) {
        val chunkSize = min(5000, totalLength - currentOffset)
        val endChunk = currentOffset + chunkSize
        val subTextAnnotated = fullText.subSequence(currentOffset, endChunk)

        val spannable = toSpannable(subTextAnnotated)

        val layout = StaticLayout.Builder.obtain(
            spannable, 0, spannable.length, textPaint, contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, lineHeightMultiplier)
            .setIncludePad(false)
            .build()

        val lineCount = layout.lineCount
        var cutOffsetInChunk = chunkSize

        for (i in 0 until lineCount) {
            if (layout.getLineBottom(i) > contentHeight) {
                val lineIndex = if (i > 0) i - 1 else 0
                cutOffsetInChunk = layout.getLineEnd(lineIndex)
                break
            }
        }

        val pageText = fullText.subSequence(currentOffset, currentOffset + cutOffsetInChunk)
        pages.add(pageText)

        currentOffset += cutOffsetInChunk

        if (pages.size % 10 == 0 || currentOffset >= totalLength) {
            emit(pages.toList())
        }
    }
}.flowOn(Dispatchers.Default)

private fun toSpannable(annotatedString: AnnotatedString): SpannableString {
    val spannable = SpannableString(annotatedString.text)

    annotatedString.spanStyles.forEach { range ->
        val style = range.item
        val start = range.start
        val end = range.end

        if (style.fontWeight == FontWeight.Bold && style.fontStyle == FontStyle.Italic) {
            spannable.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, 0)
        } else if (style.fontWeight == FontWeight.Bold) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, 0)
        } else if (style.fontStyle == FontStyle.Italic) {
            spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, 0)
        }

        if (style.textDecoration == TextDecoration.Underline) {
            spannable.setSpan(UnderlineSpan(), start, end, 0)
        }
    }
    return spannable
}