package com.example.thematiclibraryclient.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thematiclibraryclient.domain.model.quotes.QuoteDomainModel

@Composable
fun FlatQuotesList(
    quotes: List<QuoteDomainModel>,
    onQuoteClick: (QuoteDomainModel) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Цитат пока нет."
) {
    if (quotes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage)
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quotes.size) { index ->
            val quote = quotes[index]
            QuoteItem(quote = quote, onClick = { onQuoteClick(quote) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteItem(
    quote: QuoteDomainModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = "“${quote.selectedText}”")
            Spacer(Modifier.height(8.dp))
            if (quote.bookTitle.isNotEmpty()) {
                Text(
                    text = quote.bookTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}