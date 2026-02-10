package com.example.thematiclibraryclient.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.domain.model.quotes.BookGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.example.thematiclibraryclient.ui.common.AppSearchBar
import com.example.thematiclibraryclient.ui.common.ConfirmationDialog
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.common.FlatQuotesList
import com.example.thematiclibraryclient.ui.common.QuoteActionsDialog
import com.example.thematiclibraryclient.ui.common.QuoteItem
import com.example.thematiclibraryclient.ui.common.SearchScope
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.QuotesViewMode
import com.example.thematiclibraryclient.ui.viewmodel.QuotesViewModel

@Composable
fun QuotesScreen(
    navController: NavHostController,
    viewModel: QuotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var quoteToDelete by remember { mutableStateOf<QuoteDomainModel?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            currentScope = uiState.currentSearchScope,
            onScopeChange = viewModel::onSearchScopeChanged,
            availableScopes = listOf(
                SearchScope.Everywhere,
                SearchScope.Quote,
                SearchScope.Title
            ),
            onFilterClick = { },
            isFilterActive = false,
            modifier = Modifier.padding(16.dp),
            showFilterButton = false
        )

        QuoteModeTabs(
            selectedMode = uiState.currentViewMode,
            onModeSelected = { viewModel.setViewMode(it) }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val hasContent = when (uiState.currentViewMode) {
                QuotesViewMode.GROUPED -> uiState.filteredGroupedQuotes.isNotEmpty()
                QuotesViewMode.FLAT -> uiState.filteredFlatQuotes.isNotEmpty()
            }

            if (hasContent) {
                when (uiState.currentViewMode) {
                    QuotesViewMode.GROUPED -> {
                        GroupedQuotesList(
                            shelves = uiState.filteredGroupedQuotes,
                            onQuoteClick = { quote -> viewModel.onQuoteSelected(quote) }
                        )
                    }
                    QuotesViewMode.FLAT -> {
                        FlatQuotesList(
                            quotes = uiState.filteredFlatQuotes,
                            onQuoteClick = { quote -> viewModel.onQuoteSelected(quote) },
                            emptyMessage = "Цитаты не найдены."
                        )
                    }
                }
            } else if (uiState.error != null) {
                ErrorComponent(
                    errorMessage = uiState.error!!,
                    onRetry = { viewModel.refreshQuotes() }
                )
            } else if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 5.dp),
                        text = if (uiState.searchQuery.isNotEmpty()) "Ничего не найдено" else "У вас пока нет цитат или книги с цитатами пока не положены на полку",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

        }
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
private fun QuoteModeTabs(
    selectedMode: QuotesViewMode,
    onModeSelected: (QuotesViewMode) -> Unit
) {
    val tabs = listOf("По полкам", "Все цитаты")
    val selectedIndex = if (selectedMode == QuotesViewMode.GROUPED) 0 else 1

    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedIndex == index,
                onClick = {
                    val mode = if (index == 0) QuotesViewMode.GROUPED else QuotesViewMode.FLAT
                    onModeSelected(mode)
                },
                text = { Text(title) }
            )
        }
    }
}

@Composable
private fun GroupedQuotesList(
    shelves: List<ShelfGroupDomainModel>,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    if (shelves.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("У вас пока нет полок с цитатами.")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        shelves.forEach { shelf ->
            if (shelf.books.isNotEmpty()) {
                item {
                    ShelfSection(
                        shelf = shelf,
                        onQuoteClick = onQuoteClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfSection(
    shelf: ShelfGroupDomainModel,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    Column {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier
                .padding(end = 16.dp, bottom = 8.dp)
                .wrapContentWidth()
        ) {
            Text(
                text = shelf.shelfName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            shelf.books.forEach { book ->
                ExpandableBookItem(
                    book = book,
                    onQuoteClick = onQuoteClick
                )
            }
        }
    }
}

@Composable
private fun ExpandableBookItem(
    book: BookGroupDomainModel,
    onQuoteClick: (QuoteDomainModel) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val displayTitle = remember(book.bookTitle) {
        book.bookTitle
            .replace(Regex("^\\d+_"), "")
            .substringBeforeLast(".")
    }


    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ArrowRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = getQuotesCountString(book.quotes.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Развернуть",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    book.quotes.forEach { quoteGroup ->

                        val domainModel = QuoteDomainModel(
                            id = quoteGroup.id,
                            selectedText = quoteGroup.selectedText,
                            positionStart = quoteGroup.positionStart,
                            positionEnd = quoteGroup.positionEnd,
                            noteContent = quoteGroup.noteContent,
                            bookId = book.bookId,
                            bookTitle = book.bookTitle
                        )

                        QuoteItem(
                            quote = domainModel,
                            onClick = { onQuoteClick(domainModel) }
                        )
                    }
                }
            }
        }
    }
}
fun getQuotesCountString(count: Int): String {
    val remainder10 = count % 10
    val remainder100 = count % 100

    return when {
        remainder100 in 11..14 -> "$count цитат"
        remainder10 == 1 -> "$count цитата"
        remainder10 in 2..4 -> "$count цитаты"
        else -> "$count цитат"
    }
}