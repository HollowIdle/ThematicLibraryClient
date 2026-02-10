package com.example.thematiclibraryclient.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.domain.model.books.BookDetailsDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.shelves.ShelfDomainModel
import com.example.thematiclibraryclient.ui.common.AppSearchBar
import com.example.thematiclibraryclient.ui.common.ConfirmationDialog
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.FileExtensionBadge
import com.example.thematiclibraryclient.ui.common.FlatQuotesList
import com.example.thematiclibraryclient.ui.common.ManageItemsDialog
import com.example.thematiclibraryclient.ui.common.QuoteActionsDialog
import com.example.thematiclibraryclient.ui.common.SearchScope
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

    var bookmarkToEdit by remember { mutableStateOf<BookmarkDomainModel?>(null) }

    var bookmarkToDelete by remember { mutableStateOf<BookmarkDomainModel?>(null) }

    val displayTitle = remember(uiState.bookDetails?.title) {
        uiState.bookDetails?.title
            ?.replace(Regex("^\\d+_"), "")
            ?.substringBeforeLast(".")
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BookDetailsViewModel.UiEvent.NavigateBack -> navController.popBackStack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayTitle ?: "Детали книги", maxLines = 1) },
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

                    item {
                        val (currentScope, placeholder) = when (viewModel.selectedTabIndex) {
                            0 -> SearchScope.Shelf to "Поиск полки..."
                            1 -> SearchScope.Quote to "Поиск цитаты..."
                            2 -> SearchScope.Everywhere to "Поиск в заметках..."
                            else -> SearchScope.Everywhere to "Поиск..."
                        }

                        AppSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            currentScope = currentScope,
                            availableScopes = listOf(currentScope),
                            onScopeChange = {},
                            onFilterClick = {},
                            isFilterActive = false,
                            showFilterButton = false,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillParentMaxWidth(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            BookDetailsTabsHeader(
                                selectedTabIndex = viewModel.selectedTabIndex,
                                onTabSelected = { viewModel.onTabSelected(it) }
                            )
                        }
                    }

                    item {
                        Box(modifier = Modifier.fillParentMaxSize()) {
                            BookDetailsPager(
                                selectedTabIndex = viewModel.selectedTabIndex,
                                onTabSelected = { viewModel.onTabSelected(it) },
                                allShelves = uiState.filteredShelves,
                                bookDetails = uiState.bookDetails!!,
                                quotes = uiState.filteredQuotes,
                                bookmarks = uiState.filteredBookmarks,
                                onShelfMembershipChanged = { id, b -> viewModel.onShelfMembershipChanged(id, b) },
                                onQuoteClick = { q -> viewModel.onQuoteSelected(q) },
                                onBookmarkClick = { bm -> bookmarkToEdit = bm }
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
                        AdvancedTagManagerDialog(
                            currentBookTags = uiState.bookDetails!!.tags,
                            allAvailableTags = uiState.availableTags,
                            onDismiss = { activeDialog = EditDialogType.NONE },
                            onSaveBookTags = { newTags ->
                                viewModel.updateTags(newTags)
                                activeDialog = EditDialogType.NONE
                            },
                            onGlobalDelete = { tag -> viewModel.deleteTagGlobally(tag) },
                            onGlobalRename = { old, new -> viewModel.renameTagGlobally(old, new) }
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

    if (bookmarkToDelete != null) {
        ConfirmationDialog(
            title = "Удаление закладки",
            text = "Вы уверены, что хотите удалить эту закладку?",
            onConfirm = {
                viewModel.deleteBookmark(bookmarkToDelete!!)
                bookmarkToDelete = null
            },
            onDismiss = { bookmarkToDelete = null }
        )
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
            onSaveNote = { note -> viewModel.saveNoteForSelectedQuote(note.trim()) },
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

    if (bookmarkToEdit != null) {
        BookmarkActionsDialog(
            bookmark = bookmarkToEdit!!,
            onDismiss = { bookmarkToEdit = null },
            onDelete = {
                bookmarkToDelete = bookmarkToEdit
                bookmarkToEdit = null
            },
            onSaveNote = { note ->
                viewModel.updateBookmarkNote(bookmarkToEdit!!, note.trim())
                bookmarkToEdit = null
            },
            onGoTo = {
                navController.navigate(
                    ScreenRoute.Reader.createRouteWithPosition(
                        bookId,
                        bookmarkToEdit!!.position,
                        isPage = true
                    )
                )
                bookmarkToEdit = null
            }
        )
    }
}

@Composable
fun BookmarkActionsDialog(
    bookmark: BookmarkDomainModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSaveNote: (String) -> Unit,
    onGoTo: () -> Unit
) {
    var note by remember { mutableStateOf(bookmark.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Закладка (стр. ${bookmark.position + 1})") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Удалить")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSaveNote(note) }) {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onGoTo) { Text("Перейти") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedTagManagerDialog(
    currentBookTags: List<String>,
    allAvailableTags: List<String>,
    onDismiss: () -> Unit,
    onSaveBookTags: (List<String>) -> Unit,
    onGlobalDelete: (String) -> Unit,
    onGlobalRename: (String, String) -> Unit
) {
    var selectedTags by remember { mutableStateOf(currentBookTags.toSet()) }

    var searchQuery by remember { mutableStateOf("") }

    var tagToRename by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    var isSelectedTagsExpanded by remember { mutableStateOf(false) }
    val visibleTagsLimit = 3

    val filteredTags = remember(allAvailableTags, searchQuery) {
        if (searchQuery.isBlank()) allAvailableTags
        else allAvailableTags.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление тегами") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск или новый тег") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotBlank() && searchQuery !in allAvailableTags) {
                            IconButton(onClick = {
                                selectedTags = selectedTags + searchQuery.trim()
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Создать")
                            }
                        } else if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTags.isNotEmpty()) {
                    Text(
                        "Выбрано для этой книги:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val sortedSelected = remember(selectedTags) { selectedTags.sorted() }
                    val showExpandButton = sortedSelected.size > visibleTagsLimit

                    val visibleTags = if (isSelectedTagsExpanded || !showExpandButton) {
                        sortedSelected
                    } else {
                        sortedSelected.take(visibleTagsLimit)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        visibleTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { selectedTags = selectedTags - tag },
                                label = { Text(tag) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        if (showExpandButton) {
                            SuggestionChip(
                                onClick = { isSelectedTagsExpanded = !isSelectedTagsExpanded },
                                label = {
                                    Text(if (isSelectedTagsExpanded) "Свернуть" else "Ещё ${sortedSelected.size - visibleTagsLimit}")
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelectedTagsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                    HorizontalDivider()
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredTags) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tag in selectedTags,
                                onCheckedChange = { isChecked ->
                                    selectedTags = if (isChecked) selectedTags + tag else selectedTags - tag
                                }
                            )

                            Text(
                                text = tag,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            IconButton(onClick = { tagToRename = tag }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Переименовать",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { tagToDelete = tag }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить везде",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (filteredTags.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Text(
                                "Тег не найден. Нажмите +, чтобы создать.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSaveBookTags(selectedTags.toList()) }) {
                Text("Сохранить изменения")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    if (tagToDelete != null) {
        ConfirmationDialog(
            title = "Удалить тег?",
            text = "Тег \"$tagToDelete\" будет удален из ВСЕХ книг в библиотеке. Это действие нельзя отменить.",
            confirmButtonText = "Удалить везде",
            onConfirm = {
                onGlobalDelete(tagToDelete!!)
                selectedTags = selectedTags - tagToDelete!!
                tagToDelete = null
            },
            onDismiss = { tagToDelete = null }
        )
    }

    if (tagToRename != null) {
        RenameTagDialog(
            oldName = tagToRename!!,
            onDismiss = { tagToRename = null },
            onConfirm = { newName ->
                onGlobalRename(tagToRename!!, newName)
                if (tagToRename!! in selectedTags) {
                    selectedTags = (selectedTags - tagToRename!!) + newName
                }
                tagToRename = null
            }
        )
    }
}

@Composable
fun RenameTagDialog(
    oldName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(oldName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать тег") },
        text = {
            Column {
                Text("Это изменит тег во всех книгах библиотеки.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank() && text != oldName
            ) {
                Text("Переименовать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
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
    onQuoteClick: (QuoteDomainModel) -> Unit,
    onBookmarkClick: (BookmarkDomainModel) -> Unit
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
            2 -> BookmarksContent(bookmarks = bookmarks, onBookmarkClick = onBookmarkClick)
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

        if (details.fileExtension != null) {
            InfoRow(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                title = "Формат файла"
            ) {
                FileExtensionBadge(extension = details.fileExtension)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }

        InfoRowWithEdit(
            icon = Icons.Default.Person,
            title = "Авторы",
            content = details.authors.joinToString().ifEmpty { "Не указаны" },
            onEditClick = onEditAuthors
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        var isDescriptionExpanded by remember { mutableStateOf(false) }
        val descriptionText = details.description.takeIf { !it.isNullOrBlank() } ?: "Отсутствует"
        val charLimit = 150
        val shouldShowExpand = descriptionText.length > charLimit

        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Описание",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (shouldShowExpand && !isDescriptionExpanded) {
                        descriptionText.take(charLimit) + "..."
                    } else {
                        descriptionText
                    },
                    style = MaterialTheme.typography.bodyLarge
                )

                if (shouldShowExpand) {
                    Text(
                        text = if (isDescriptionExpanded) "Свернуть" else "Читать далее",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            .padding(vertical = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEditDescription) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать описание")
            }
        }

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
        EmptyTabMessage("У вас еще нет полок (или ни одна не соответствует поиску).")
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
private fun EmptyTabMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuotesContent(
    quotes: List<QuoteDomainModel>,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    if (quotes.isEmpty()) {
        EmptyTabMessage("Цитаты не найдены.")
    } else {
        FlatQuotesList(
            quotes = quotes,
            onQuoteClick = onQuoteClick,
        )
    }
}

@Composable
private fun BookmarksContent(
    bookmarks: List<BookmarkDomainModel>,
    onBookmarkClick: (BookmarkDomainModel) -> Unit
) {
    if (bookmarks.isEmpty()) {
        EmptyTabMessage("Закладки не найдены.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bookmarks.size) { index ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onBookmarkClick(bookmarks[index]) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Закладка на странице ${ bookmarks[index].position + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!bookmarks[index].note.isNullOrBlank()) {
                            Text(
                                text = bookmarks[index].note!!,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
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
    val maxCharCount = 500

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать описание") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= maxCharCount) text = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    label = { Text("Описание") },
                    placeholder = { Text("Введите описание книги...") },
                    singleLine = false,
                    maxLines = 10,
                    supportingText = {
                        Text(
                            text = "${text.length} / $maxCharCount",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.trim()) }) {
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
private fun InfoRow(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}