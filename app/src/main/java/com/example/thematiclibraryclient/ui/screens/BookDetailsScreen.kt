package com.example.thematiclibraryclient.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.ui.common.ConfirmationDialog
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.FlatQuotesList
import com.example.thematiclibraryclient.ui.common.ManageItemsDialog
import com.example.thematiclibraryclient.ui.common.QuoteActionsDialog
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.BookDetailsViewModel

private enum class EditDialogType {
    NONE, AUTHORS, TAGS, DESCRIPTION
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: Int,
    navController: NavHostController,
    viewModel: BookDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeDialog by remember { mutableStateOf(EditDialogType.NONE) }

    var quoteToDelete by remember { mutableStateOf<QuoteDomainModel?>(null) }
    var showDeleteBookDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BookDetailsViewModel.UiEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bookDetails?.title ?: "Детали книги", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteBookDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить книгу", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.bookDetails != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        BookDetailsContent(
                            details = uiState.bookDetails!!,
                            onEditAuthors = { activeDialog = EditDialogType.AUTHORS },
                            onEditTags = { activeDialog = EditDialogType.TAGS },
                            onEditDescription = { activeDialog = EditDialogType.DESCRIPTION },
                            onReadClick = {
                                navController.navigate(ScreenRoute.Reader.createRoute(bookId))
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillParentMaxWidth(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            BookDetailsTabsHeader(
                                selectedTabIndex = viewModel.selectedTabIndex, // Нужно добавить в VM
                                onTabSelected = { viewModel.onTabSelected(it) }
                            )
                        }
                    }

                    item {
                        Box(modifier = Modifier.fillParentMaxSize()) {
                            BookDetailsPager(
                                selectedTabIndex = viewModel.selectedTabIndex,
                                onTabSelected = { viewModel.onTabSelected(it) },
                                allShelves = uiState.allShelves,
                                bookDetails = uiState.bookDetails!!,
                                quotes = uiState.quotes,
                                bookmarks = uiState.bookmarks,
                                onShelfMembershipChanged = { id, b -> viewModel.onShelfMembershipChanged(id, b) },
                                onQuoteClick = { q -> viewModel.onQuoteSelected(q) }
                            )
                        }
                    }
                }

                when (activeDialog) {
                    EditDialogType.AUTHORS -> {
                        ManageItemsDialog(
                            title = "Управление авторами",
                            initialItems = uiState.bookDetails!!.authors,
                            onDismiss = { activeDialog = EditDialogType.NONE },
                            onSave = { newAuthors ->
                                viewModel.updateAuthors(newAuthors)
                                activeDialog = EditDialogType.NONE
                            }
                        )
                    }
                    EditDialogType.TAGS -> {
                        ManageItemsDialog(
                            title = "Управление тегами",
                            initialItems = uiState.bookDetails!!.tags,
                            onDismiss = { activeDialog = EditDialogType.NONE },
                            onSave = { newTags ->
                                viewModel.updateTags(newTags)
                                activeDialog = EditDialogType.NONE
                            }
                        )
                    }
                    EditDialogType.DESCRIPTION -> {
                        EditDescriptionDialog(
                            initialValue = uiState.bookDetails!!.description ?: "",
                            onDismiss = { activeDialog = EditDialogType.NONE },
                            onConfirm = { text ->
                                viewModel.updateDescription(text)
                                activeDialog = EditDialogType.NONE
                            }
                        )
                    }
                    EditDialogType.NONE -> {}
                }

            } else if (uiState.error != null) {
                ErrorComponent(
                    errorMessage = uiState.error!!,
                    onRetry = { viewModel.refreshBookDetails() }
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

        }
    }

    if (showDeleteBookDialog) {
        ConfirmationDialog(
            title = "Удаление книги",
            text = "Вы уверены? Это действие нельзя отменить.",
            onConfirm = {
                viewModel.deleteBook()
                showDeleteBookDialog = false
            },
            onDismiss = { showDeleteBookDialog = false }
        )
    }

    if (uiState.selectedQuote != null) {
        QuoteActionsDialog(
            quote = uiState.selectedQuote!!,
            onDismiss = { viewModel.onDialogDismiss() },
            onDelete = {
                quoteToDelete = uiState.selectedQuote
                viewModel.onDialogDismiss()
            },
            onSaveNote = { note -> viewModel.saveNoteForSelectedQuote(note) },
            onGoToSource = { quote ->
                navController.navigate(
                    ScreenRoute.Reader.createRouteWithPosition(quote.bookId, quote.positionStart)
                )
                viewModel.onDialogDismiss()
            }
        )
    }

    if (quoteToDelete != null) {
        ConfirmationDialog(
            title = "Удаление цитаты",
            text = "Вы уверены, что хотите удалить эту цитату?",
            onConfirm = {
                viewModel.deleteQuote(quoteToDelete!!)
                quoteToDelete = null
            },
            onDismiss = { quoteToDelete = null }
        )
    }
}

@Composable
fun BookDetailsTabsHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Полки", "Цитаты", "Закладки")

    TabRow(selectedTabIndex = selectedTabIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookDetailsPager(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    allShelves: List<ShelfDomainModel>,
    bookDetails: BookDetailsDomainModel,
    quotes: List<QuoteDomainModel>,
    bookmarks: List<BookmarkDomainModel>,
    onShelfMembershipChanged: (Int, Boolean) -> Unit,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    LaunchedEffect(selectedTabIndex) {
        pagerState.animateScrollToPage(selectedTabIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTabIndex) {
            onTabSelected(pagerState.currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top
    ) { page ->
        when (page) {
            0 -> ShelvesContent(
                allShelves = allShelves,
                bookShelfIds = bookDetails.shelfIds.toSet(),
                onCheckedChange = onShelfMembershipChanged
            )
            1 -> QuotesContent(quotes = quotes, onQuoteClick = onQuoteClick)
            2 -> BookmarksContent(bookmarks = bookmarks)
        }
    }
}

@Composable
private fun BookDetailsContent(
    details: BookDetailsDomainModel,
    onEditAuthors: () -> Unit,
    onEditTags: () -> Unit,
    onEditDescription: () -> Unit,
    onReadClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

        InfoRowWithEdit(
            icon = Icons.Default.Person,
            title = "Авторы",
            content = details.authors.joinToString().ifEmpty { "Не указаны" },
            onEditClick = onEditAuthors
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        InfoRowWithEdit(
            icon = Icons.Default.Description,
            title = "Описание",
            content = details.description.takeIf { !it.isNullOrBlank() } ?: "Отсутствует",
            onEditClick = onEditDescription
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Теги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        TagsFlowRow(
            tags = details.tags,
            onAddTag = onEditTags
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onReadClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Читать книгу", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ShelvesContent(
    allShelves: List<ShelfDomainModel>,
    bookShelfIds: Set<Int>,
    onCheckedChange: (shelfId: Int, isChecked: Boolean) -> Unit
) {
    if (allShelves.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("У вас еще нет полок. Создайте их в разделе 'Управление полками'.")
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(allShelves.size) { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isChecked = bookShelfIds.contains(allShelves[index].id)
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckedChange(allShelves[index].id, it) }
                )
                Text(allShelves[index].name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun QuotesContent(
    quotes: List<QuoteDomainModel>,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    FlatQuotesList(
        quotes = quotes,
        onQuoteClick = onQuoteClick,
        emptyMessage = "Для этой книги еще нет цитат."
    )
}

@Composable
private fun BookmarksContent(bookmarks: List<BookmarkDomainModel>) {
    if (bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Для этой книги еще нет закладок.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bookmarks.size) { index ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Закладка на странице ${ bookmarks[index].position + 1}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRowWithEdit(
    icon: ImageVector,
    title: String,
    content: String,
    onEditClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }
        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Редактировать $title")
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFlowRow(
    tags: List<String>,
    onAddTag: () -> Unit,
    initialCount: Int = 5
) {
    var isExpanded by remember { mutableStateOf(false) }

    val showExpandButton = tags.size > initialCount

    val visibleTags = if (isExpanded || !showExpandButton) {
        tags
    } else {
        tags.take(initialCount)
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleTags.forEach { tag ->
            SuggestionChip(
                onClick = { },
                label = { Text(tag) }
            )
        }

        if (showExpandButton) {
            SuggestionChip(
                onClick = { isExpanded = !isExpanded },
                label = {
                    Text(if (isExpanded) "Свернуть" else "Ещё ${tags.size - initialCount}")
                },
                icon = {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        SuggestionChip(
            onClick = { onAddTag() },
            label = { Icon(Icons.Default.Edit, contentDescription = "Добавить тег") },
            shape = RoundedCornerShape(16.dp),
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

@Composable
fun EditDescriptionDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать описание") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("Описание") },
                placeholder = { Text("Введите описание книги...") },
                singleLine = false,
                maxLines = 10
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
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