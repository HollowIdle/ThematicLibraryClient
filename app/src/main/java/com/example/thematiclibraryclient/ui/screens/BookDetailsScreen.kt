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
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.BookDetailsViewModel
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: Int,
    navController: NavHostController,
    viewModel: BookDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bookDetails?.title ?: "Детали книги", maxLines = 1) },
                // TODO: Добавить кнопку "Редактировать"
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
                            BookDetailsContent(details = uiState.bookDetails!!)
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookDetailsTabs(
    allShelves: List<ShelfDomainModel>,
    bookDetails: BookDetailsDomainModel,
    onShelfMembershipChanged: (Int, Boolean) -> Unit,
    quotes: List<QuoteDomainModel>,
    bookmarks: List<BookmarkDomainModel>
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
                1 -> QuotesContent(quotes = quotes)
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
private fun QuotesContent(quotes: List<QuoteDomainModel>) {
    if (quotes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Для этой книги еще нет цитат.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(quotes.size) { index ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "“${quotes[index].selectedText}”",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
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
private fun BookDetailsContent(details: BookDetailsDomainModel) {
    Column {
        Text(details.title, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        InfoRow(icon = Icons.Default.Person, title = "Авторы", content = details.authors.joinToString())
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        InfoRow(
            icon = Icons.Default.Description,
            title = "Описание",
            content = details.description ?: "Описание отсутствует"
        )
        // TODO: Добавить кнопку "Добавить/Изменить описание"
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Теги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        TagsFlowRow(tags = details.tags)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, content: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFlowRow(tags: List<String>) {
    FlowRow(
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        tags.forEach { tag ->
            SuggestionChip(
                onClick = { /* TODO: Действие по клику на тег */ },
                label = { Text(tag) }
            )
        }
        SuggestionChip(
            onClick = { /* TODO: Показать диалог добавления тега */ },
            label = { Icon(Icons.Default.Add, contentDescription = "Добавить тег") },
            shape = RoundedCornerShape(16.dp)
        )
    }
}