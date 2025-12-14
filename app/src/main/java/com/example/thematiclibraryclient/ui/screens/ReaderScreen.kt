package com.example.thematiclibraryclient.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.Paginator
import com.example.thematiclibraryclient.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    initialPosition: Int,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    //Primary
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    //Pagination
    var showPagePickerDialog by remember { mutableStateOf(false) }

    //Quote Note
    var showQuoteDialog by remember { mutableStateOf(false) }
    var quoteDataToCreate by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    //Bookmark
    val isBookmarked = uiState.bookmarks.any { it.position == uiState.currentPage }

    //Style
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5,
        textAlign = TextAlign.Justify
    )
    val pagePadding = PaddingValues(16.dp)


    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ReaderViewModel.UiEvent.ShowSnackbar -> {
                    snackBarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    LaunchedEffect(key1 = bookId) {
        if (uiState.fullContent == null && !uiState.isLoading) {
            viewModel.loadBook(bookId)
        }
    }

    DisposableEffect(key1 = bookId) {
        onDispose {
            if (initialPosition == -1) {
                viewModel.saveProgress(bookId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.pages.isEmpty()) "Расчет страниц..."
                        else if (uiState.isInSelectionMode) "Выделите текст"
                        else "Стр. ${uiState.currentPage + 1} / ${uiState.pages.size}",
                        modifier = Modifier.clickable(
                            enabled = uiState.pages.isNotEmpty(),
                            onClick = { showPagePickerDialog = true }
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        Icon(
                            imageVector = if (uiState.isInSelectionMode) Icons.Default.Close else Icons.Default.FormatQuote,
                            contentDescription = "Режим цитирования"
                        )
                    }
                    if (!uiState.isInSelectionMode) {
                        IconButton(onClick = { viewModel.toggleBookmark(bookId) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Добавить/удалить закладку",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val content = uiState.fullContent

                if (content != null) {
                    if (uiState.pages.isEmpty()) {
                        Paginator(
                            fullText = content,
                            style = textStyle,
                            contentPadding = pagePadding,
                            onPagesCalculated = { pages ->
                                viewModel.onPagesCalculated(pages)
                            }
                        )
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        val startPage = remember(uiState.pages, initialPosition) {
                            if (initialPosition != -1) {
                                var charCount = 0
                                var target = 0
                                for ((index, page) in uiState.pages.withIndex()) {
                                    if (initialPosition < charCount + page.length) {
                                        target = index
                                        break
                                    }
                                    charCount += page.length
                                }
                                target
                            } else {
                                uiState.currentPage
                            }
                        }

                        val pagerState = rememberPagerState(
                            initialPage = startPage,
                            pageCount = { uiState.pages.size }
                        )

                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.currentPage }.collect { page ->
                                viewModel.onPageChanged(page)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !uiState.isInSelectionMode
                        ) { pageIndex ->
                            if (uiState.isInSelectionMode && pageIndex == uiState.currentPage) {
                                SelectionPageContent(
                                    text = uiState.pages[pageIndex],
                                    style = textStyle,
                                    padding = pagePadding,
                                    onQuoteCreateRequested = { selectedText, start, end ->
                                        quoteDataToCreate = Triple(selectedText, start, end)
                                        showQuoteDialog = true
                                    }
                                )
                            } else {
                                ReadingPageContent(
                                    text = uiState.pages[pageIndex],
                                    style = textStyle,
                                    padding = pagePadding
                                )
                            }
                        }

                        if (showPagePickerDialog) {
                            PagePickerDialog(
                                pageCount = uiState.pages.size,
                                onPageSelected = { pageIndex ->
                                    scope.launch {
                                        pagerState.scrollToPage(pageIndex)
                                    }
                                    showPagePickerDialog = false
                                },
                                onDismiss = { showPagePickerDialog = false }
                            )
                        }
                    }
                } else if (uiState.error != null) {
                    ErrorComponent(
                        errorMessage = uiState.error!!,
                        onRetry = { viewModel.loadBook(bookId) }
                    )
                }
            }
        }
    }

    if (showQuoteDialog && quoteDataToCreate != null) {
        CreateQuoteDialog(
            quoteText = quoteDataToCreate!!.first,
            onDismiss = {
                showQuoteDialog = false
                quoteDataToCreate = null
            },
            onConfirm = { note ->
                val (text, start, end) = quoteDataToCreate!!
                viewModel.createQuote(bookId, text, start, end, note)
                showQuoteDialog = false
                quoteDataToCreate = null
                viewModel.toggleSelectionMode()
            }
        )
    }

}
@Composable
fun CreateQuoteDialog(
    quoteText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать цитату") },
        text = {
            Column {
                Text(
                    text = "“$quoteText”",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ваша заметка (необязательно)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(note) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
@Composable
private fun PagePickerDialog(
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейти к странице") },
        text = {
            val listState = rememberLazyListState()
            LazyColumn(state = listState) {
                items(pageCount) { index ->
                    Text(
                        text = "Страница ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPageSelected(index) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ReadingPageContent(
    text: String,
    style: TextStyle,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Text(
            text = text,
            style = style
        )
    }
}

@Composable
private fun SelectionPageContent(
    text: String,
    style: TextStyle,
    padding: PaddingValues,
    onQuoteCreateRequested: (String, Int, Int) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text))
    }
    val selection = textFieldValue.selection
    val hasSelection = !selection.collapsed

    Box(
        modifier = Modifier.fillMaxSize()
        .padding(padding)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            readOnly = true,
            modifier = Modifier
                .fillMaxSize(),
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )

        if (hasSelection) {
            FloatingActionButton(
                onClick = {
                    val start = minOf(selection.start, selection.end)
                    val end = maxOf(selection.start, selection.end)
                    val selectedText = textFieldValue.text.substring(start, end)
                    onQuoteCreateRequested(selectedText, start, end)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.FormatQuote, contentDescription = "Цитировать")
            }
        }
    }
}

