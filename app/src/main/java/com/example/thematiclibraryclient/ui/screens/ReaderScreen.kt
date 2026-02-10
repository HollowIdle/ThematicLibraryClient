package com.example.thematiclibraryclient.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.data.local.source.PaginationCache
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.Paginator
import com.example.thematiclibraryclient.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    initialPosition: Int,
    isPageNavigation: Boolean,
    navController: NavHostController,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    var showQuoteDialog by remember { mutableStateOf(false) }
    var quoteTextToCreate by remember { mutableStateOf("") }
    var quoteStart by remember { mutableStateOf(0) }
    var quoteEnd by remember { mutableStateOf(0) }

    var showPagePickerDialog by remember { mutableStateOf(false) }

    val currentBookmark = uiState.bookmarks.find { it.position == uiState.currentPage }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    val isBookmarked = uiState.bookmarks.any { it.position == uiState.currentPage }

    var pdfSelectedText by remember { mutableStateOf<String?>(null) }

    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5,
        textAlign = TextAlign.Justify
    )
    val pagePadding = PaddingValues(16.dp)

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ReaderViewModel.UiEvent.ShowSnackbar -> snackBarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(uiState.isInSelectionMode) {
        if (!uiState.isInSelectionMode) {
            pdfSelectedText = null
        }
    }

    LaunchedEffect(key1 = bookId) {
        if (uiState.fullContent == null && uiState.pdfPath == null && !uiState.isLoading) {
            viewModel.loadBook(bookId, initialPosition, isPageNavigation)
        }
    }

    DisposableEffect(key1 = bookId) {
        onDispose {
            if (initialPosition == -1) viewModel.saveProgress(bookId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isLoading) "Загрузка..."
                        else if (uiState.isPdfMode) "Стр. ${uiState.currentPage + 1}"
                        else "Стр. ${uiState.currentPage + 1} / ${uiState.pages.size}",
                        modifier = Modifier.clickable(enabled = !uiState.isPdfMode && uiState.pages.isNotEmpty()) {
                            showPagePickerDialog = true
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        Icon(
                            imageVector = if (uiState.isInSelectionMode) Icons.Default.Close else Icons.Default.FormatQuote,
                            contentDescription = "Режим цитирования"
                        )
                    }
                    IconButton(onClick = {
                        if (isBookmarked) {
                            viewModel.deleteBookmark(currentBookmark!!.id)
                        } else {
                            showBookmarkDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            contentDescription = "Закладка"
                        )
                    }
                }
            )

        },
        floatingActionButton = {
            if (uiState.isPdfMode && pdfSelectedText != null && uiState.isInSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        quoteTextToCreate = pdfSelectedText!!
                        showQuoteDialog = true
                    },
                    icon = { Icon(Icons.Default.FormatQuote, "Создать цитату") },
                    text = { Text("Создать цитату") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                ErrorComponent(errorMessage = uiState.error!!, onRetry = { viewModel.loadBook(bookId) })
            } else {

                if (uiState.isPdfMode && uiState.pdfPath != null) {
                    PdfJsReaderScreen(
                        filePath = uiState.pdfPath!!,
                        currentPage = uiState.currentPage,
                        requestedPage = uiState.requestedPage,
                        isInSelectionMode = uiState.isInSelectionMode,
                        onSelectionChanged = { text ->
                            pdfSelectedText = text
                        },
                        onPageChanged = { page -> viewModel.onPageChanged(page) }
                    )
                }   else if (uiState.fullContent != null) {
                    TextReaderContent(
                        bookId = bookId,
                        uiState = uiState,
                        textStyle = textStyle,
                        pagePadding = pagePadding,
                        paginationCache = viewModel.paginationCache,
                        onPagesUpdated = viewModel::onPagesUpdated,
                        onPageChanged = viewModel::onPageChanged,
                        onQuoteCreateRequested = { text, start, end ->
                            quoteTextToCreate = text
                            quoteStart = start
                            quoteEnd = end
                            showQuoteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showBookmarkDialog) {
        CreateBookmarkDialog(
            onDismiss = { showBookmarkDialog = false },
            onConfirm = { note ->
                viewModel.createBookmark(bookId, uiState.currentPage, note)
                showBookmarkDialog = false
            },
            currentPage = uiState.currentPage
        )
    }

    if (showQuoteDialog) {
        CreateQuoteDialog(
            quoteText = quoteTextToCreate,
            onDismiss = { showQuoteDialog = false },
            onConfirm = { note ->
                viewModel.createQuote(bookId, quoteTextToCreate, quoteStart, quoteEnd, note)
                showQuoteDialog = false
                if (uiState.isInSelectionMode) viewModel.toggleSelectionMode()
            }
        )
    }

    if (showPagePickerDialog) {
        PagePickerDialog(
            pageCount = uiState.pages.size,
            onPageSelected = { pageIndex ->
                viewModel.jumpToPage(pageIndex)
                showPagePickerDialog = false
            },
            onDismiss = { showPagePickerDialog = false }
        )
    }
}

@Composable
fun CreateBookmarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    currentPage: Int
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать закладку") },
        text = {
            Column {
                Text(
                    text = "Вы хотите создать закладку на ${currentPage+1} странице.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(if (note.isBlank()) null else note.trim()) }) {
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
fun TextReaderContent(
    bookId: Int,
    uiState: com.example.thematiclibraryclient.ui.viewmodel.ReaderUiState,
    textStyle: TextStyle,
    pagePadding: PaddingValues,
    paginationCache: PaginationCache,
    onPagesUpdated: (List<AnnotatedString>) -> Unit,
    onPageChanged: (Int) -> Unit,
    onQuoteCreateRequested: (String, Int, Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage && uiState.pages.isNotEmpty()) {
            pagerState.scrollToPage(uiState.currentPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    Paginator(
        bookId = bookId,
        fullText = uiState.fullContent,
        style = textStyle,
        paginationCache = paginationCache,
        contentPadding = pagePadding,
        onPagesUpdated = onPagesUpdated
    )

    if (uiState.pages.isNotEmpty()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !uiState.isInSelectionMode
        ) { pageIndex ->
            if (pageIndex < uiState.pages.size) {
                if (uiState.isInSelectionMode && pageIndex == uiState.currentPage) {
                    SelectionPageContent(
                        text = uiState.pages[pageIndex],
                        style = textStyle,
                        padding = pagePadding,
                        onQuoteCreateRequested = onQuoteCreateRequested
                    )
                } else {
                    ReadingPageContent(
                        text = uiState.pages[pageIndex],
                        style = textStyle,
                        padding = pagePadding
                    )
                }
            }
        }
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
            Button(onClick = { onConfirm(note.trim()) }) {
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
    text: AnnotatedString,
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
    text: AnnotatedString,
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
            ExtendedFloatingActionButton(
                onClick = {
                    val start = minOf(selection.start, selection.end)
                    val end = maxOf(selection.start, selection.end)
                    val selectedText = textFieldValue.text.substring(start, end)
                    onQuoteCreateRequested(selectedText, start, end)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                        icon = { Icon(Icons.Default.FormatQuote, "Создать цитату") },
                        text = { Text("Создать цитату") },
            )
        }
    }
}