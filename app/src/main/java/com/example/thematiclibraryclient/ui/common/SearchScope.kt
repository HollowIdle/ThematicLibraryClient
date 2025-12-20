package com.example.thematiclibraryclient.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SearchScope(val title: String, val icon: ImageVector) {
    data object Everywhere : SearchScope("Везде", Icons.Default.Search)
    data object Title : SearchScope("Название", Icons.Default.Title)
    data object Author : SearchScope("Автор", Icons.Default.Person)
    data object Tag : SearchScope("Тег", Icons.Default.Tag)
    data object Shelf : SearchScope("Полка", Icons.Default.Folder)
    data object Quote : SearchScope("Текст", Icons.Default.FormatQuote)
}