package com.example.thematiclibraryclient.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.thematiclibraryclient.ui.common.QuoteActionsDialog
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.BookDetailsViewModel
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch

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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf(EditDialogType.NONE) }
    var quoteToDelete by remember { mutableStateOf<QuoteDomainModel?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BookDetailsViewModel.UiEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                is BookDetailsViewModel.UiEvent.ShowSnackbar -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bookDetails?.title ?: "Детали книги", maxLines = 1) },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить книгу",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.bookDetails != null) {
                ExtendedFloatingActionButton(
                    text = { Text("Читать") },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    onClick = {
                        navController.navigate(ScreenRoute.Reader.createRoute(bookId))
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    ErrorComponent(
                        errorMessage = uiState.error!!,
                        onRetry = { viewModel.loadBookDetails() }
                    )
                }
                uiState.bookDetails != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 80.dp
                        )
                    ) {
                        item {
                            BookDetailsContent(
                                details = uiState.bookDetails!!,
                                onEditAuthors = { activeDialog = EditDialogType.AUTHORS },
                                onEditTags = { activeDialog = EditDialogType.TAGS },
                                onEditDescription = { activeDialog = EditDialogType.DESCRIPTION }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        stickyHeader {
                            Surface(modifier = Modifier.fillParentMaxWidth()) {
                                BookDetailsTabs(
                                    allShelves = uiState.allShelves,
                                    bookDetails = uiState.bookDetails!!,
                                    quotes = uiState.quotes,
                                    bookmarks = uiState.bookmarks,
                                    onShelfMembershipChanged = { shelfId, belongs ->
                                        viewModel.onShelfMembershipChanged(shelfId, belongs)
                                    },
                                    onQuoteClick = { quote ->
                                        viewModel.onQuoteSelected(quote)
                                    }
                                )
                            }
                        }
                    }
                    when (activeDialog) {
                        EditDialogType.AUTHORS -> {
                            EditListDialog(
                                title = "Редактировать авторов",
                                initialValue = uiState.bookDetails?.authors?.joinToString(", ") ?: "",
                                onDismiss = { activeDialog = EditDialogType.NONE },
                                onConfirm = { text ->
                                    val authors = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                    viewModel.updateAuthors(authors)
                                    activeDialog = EditDialogType.NONE
                                }
                            )
                        }
                        EditDialogType.TAGS -> {
                            EditListDialog(
                                title = "Редактировать теги",
                                initialValue = uiState.bookDetails?.tags?.joinToString(", ") ?: "",
                                onDismiss = { activeDialog = EditDialogType.NONE },
                                onConfirm = { text ->
                                    val tags = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                    viewModel.updateTags(tags)
                                    activeDialog = EditDialogType.NONE
                                }
                            )
                        }
                        EditDialogType.DESCRIPTION -> {
                            EditDescriptionDialog(
                                initialValue = uiState.bookDetails?.description ?: "",
                                onDismiss = { activeDialog = EditDialogType.NONE },
                                onConfirm = { text ->
                                    viewModel.updateDescription(text)
                                    activeDialog = EditDialogType.NONE
                                }
                            )
                        }
                        EditDialogType.NONE -> {}
                    }
                }
            }
        }
    }
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Удаление книги",
            text = "Вы уверены, что хотите удалить эту книгу? Все цитаты и закладки также будут удалены.",
            onConfirm = {
                viewModel.deleteBook()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookDetailsTabs(
    allShelves: List<ShelfDomainModel>,
    bookDetails: BookDetailsDomainModel,
    quotes: List<QuoteDomainModel>,
    bookmarks: List<BookmarkDomainModel>,
    onShelfMembershipChanged: (Int, Boolean) -> Unit,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    val tabs = listOf("Полки", "Цитаты", "Закладки")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
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
private fun BookDetailsContent(
    details: BookDetailsDomainModel,
    onEditAuthors: () -> Unit,
    onEditTags: () -> Unit,
    onEditDescription: () -> Unit
) {
    Column {
        Text(details.title, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

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
    onAddTag: () -> Unit
    ) {
    FlowRow(
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        tags.forEach { tag ->
            SuggestionChip(
                onClick = { onAddTag() },
                label = { Text(tag) }
            )
        }
        SuggestionChip(
            onClick = { onAddTag() },
            label = { Icon(Icons.Default.Add, contentDescription = "Добавить тег") },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun EditListDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Введите значения через запятую", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
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