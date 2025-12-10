package com.example.thematiclibraryclient.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Paginator(
    fullText: String,
    style: TextStyle,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPagesCalculated: (List<String>) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val textMeasurer = rememberTextMeasurer()
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

        LaunchedEffect(fullText, contentWidth, contentHeight, style) {
            if (fullText.isNotEmpty() && contentWidth > 0 && contentHeight > 0) {
                val pages = paginate(
                    fullText = fullText,
                    textMeasurer = textMeasurer,
                    style = style,
                    contentWidth = contentWidth,
                    contentHeight = contentHeight
                )
                onPagesCalculated(pages)
            }
        }
    }
}

private suspend fun paginate(
    fullText: String,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    contentWidth: Int,
    contentHeight: Int
): List<String> = withContext(Dispatchers.Default) {
    val pages = mutableListOf<String>()
    var currentOffset = 0

    val constraints = Constraints(maxWidth = contentWidth)

    while (currentOffset < fullText.length) {
        val remainingText = fullText.substring(currentOffset)

        val layoutResult = textMeasurer.measure(
            text = remainingText,
            style = style,
            constraints = constraints
        )

        if (layoutResult.size.height <= contentHeight) {
            pages.add(remainingText)
            break
        }

        var lastVisibleLineIndex = -1
        for (i in 0 until layoutResult.lineCount) {
            if (layoutResult.getLineBottom(i) > contentHeight) {
                break
            }
            lastVisibleLineIndex = i
        }

        if (lastVisibleLineIndex == -1) {
            lastVisibleLineIndex = 0
        }

        var endOffset = layoutResult.getLineEnd(lastVisibleLineIndex, visibleEnd = false)

        if (endOffset <= 0) {
            break
        }

        // Фиксим обрезание предложений
        val chunk = remainingText.substring(0, endOffset)
        val lastSentenceEnd = chunk.lastIndexOfAny(charArrayOf('.', '!', '?', '…'))

        if (lastSentenceEnd == -1 || lastSentenceEnd < chunk.length - 2) {
            val lastFinishedSentenceIndex = chunk.substring(0, lastSentenceEnd + 1).lastIndexOfAny(charArrayOf('.', '!', '?', '…'))

            if (lastFinishedSentenceIndex != -1) {
                endOffset = lastFinishedSentenceIndex + 1
            }
        }

        val pageText = remainingText.substring(0, endOffset)
        pages.add(pageText)

        currentOffset += endOffset
    }
    return@withContext pages
}