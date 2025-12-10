package com.example.thematiclibraryclient.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.thematiclibraryclient.domain.model.books.BookmarkDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.BookGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteFlatDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.QuoteGroupDomainModel
import com.example.thematiclibraryclient.domain.model.quotes.ShelfGroupDomainModel
import com.example.thematiclibraryclient.ui.common.ErrorComponent
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.viewmodel.QuotesViewMode
import com.example.thematiclibraryclient.ui.viewmodel.QuotesViewModel

@Composable
fun QuotesScreen(
    navController: NavHostController,
    viewModel: QuotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        QuoteModeTabs(
            selectedMode = uiState.currentViewMode,
            onModeSelected = { viewModel.setViewMode(it) }
        )

        Box(
            modifier = Modifier.weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                        )
                }
                uiState.error != null -> {
                    ErrorComponent(
                        errorMessage = uiState.error!!,
                        onRetry = {viewModel.loadQuotes()}
                    )
                }
                uiState.currentViewMode == QuotesViewMode.GROUPED -> {
                    GroupedQuotesList(shelves = uiState.groupedQuotes)
                }
                uiState.currentViewMode == QuotesViewMode.FLAT -> {
                    FlatQuotesList(
                        quotes = uiState.flatQuotes,
                        onQuoteClick = { quote ->
                            viewModel.onQuoteSelected(quote)
                        }
                    )
                }
            }
        }
    }

    if (uiState.selectedQuote != null) {
        QuoteActionsDialog(
            quote = uiState.selectedQuote!!,
            onDismiss = { viewModel.onDialogDismiss() },
            onDelete = { viewModel.deleteSelectedQuote() },
            onSaveNote = { note -> viewModel.saveNoteForSelectedQuote(note) },
            onGoToSource = { quote ->
                navController.navigate(
                    ScreenRoute.Reader.createRouteWithPosition(quote.bookId, quote.positionStart)
                )
                viewModel.onDialogDismiss()
            }
        )
    }

}

@Composable
fun QuoteActionsDialog(
    quote: QuoteFlatDomainModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSaveNote: (String) -> Unit,
    onGoToSource: (QuoteFlatDomainModel) -> Unit
) {
    var noteText by remember { mutableStateOf(quote.noteContent ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление цитатой") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "“${quote.selectedText}”",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Заметка к цитате") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSaveNote(noteText) }) {
                        Text("Сохранить")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                    TextButton(onClick = { onGoToSource(quote) }) {
                        Text("Перейти к источнику")
                    }
                }
            }
        }
    )
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
private fun FlatQuotesList(
    quotes: List<QuoteFlatDomainModel>,
    onQuoteClick: (QuoteFlatDomainModel) -> Unit
) {
    if (quotes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("У вас пока нет ни одной цитаты.")
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(quotes.size) { index ->
            val quote = quotes[index]
            FlatQuoteItem(quote = quote, onClick = { onQuoteClick(quote)})
        }
    }
}

@Composable
private fun FlatQuoteItem(
    quote: QuoteFlatDomainModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "“${quote.selectedText}”",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = quote.bookTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun GroupedQuotesList(shelves: List<ShelfGroupDomainModel>) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        shelves.forEach { shelf ->
            item {
                ShelfHeader(name = shelf.shelfName)
            }
            shelf.books.forEach { book ->
                item {
                    BookItemWithQuotes(book = book)
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun BookItemWithQuotes(book: BookGroupDomainModel) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 16.dp)) {
        Text(
            text = book.bookTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        book.quotes.forEach { quote ->
            QuoteItem(quote = quote)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun QuoteItem(quote: QuoteGroupDomainModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "“${quote.selectedText}”",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}