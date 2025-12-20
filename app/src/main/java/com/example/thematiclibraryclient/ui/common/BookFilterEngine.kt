package com.example.thematiclibraryclient.ui.common

import com.example.thematiclibraryclient.domain.model.books.AuthorDomainModel
import com.example.thematiclibraryclient.domain.model.books.BookDomainModel
import com.example.thematiclibraryclient.ui.model.BookFilterState
import com.example.thematiclibraryclient.ui.common.SearchScope
import javax.inject.Inject

class BookFilterEngine @Inject constructor() {

    fun filter(
        books: List<BookDomainModel>,
        query: String,
        scope: SearchScope,
        filterState: BookFilterState
    ): List<BookDomainModel> {
        val trimmedQuery = query.trim()

        return books.filter { book ->
            val matchesSearch = if (trimmedQuery.isBlank()) true else {
                when (scope) {
                    SearchScope.Everywhere -> {
                        book.title.contains(trimmedQuery, ignoreCase = true) ||
                                book.authors.map { it?.name }.any { it?.contains(query, ignoreCase = true) == true } ||
                                book.tags.any { it.contains(trimmedQuery, ignoreCase = true) }
                    }
                    SearchScope.Title -> book.title.contains(trimmedQuery, ignoreCase = true)
                    SearchScope.Author -> book.authors.map { it?.name }.any { it?.contains(query, ignoreCase = true) == true }
                    SearchScope.Tag -> book.tags.any { it.contains(trimmedQuery, ignoreCase = true) }
                    else -> true
                }
            }

            val matchesAuthors = if (filterState.selectedAuthors.isEmpty()) true else {
                book.authors.map { it?.name }.any { it in filterState.selectedAuthors }
                filterState.selectedTags.all { selectedTag ->
                    book.tags.contains(selectedTag)
                }
            }
            val matchesTags = if (filterState.selectedTags.isEmpty()) true else {
                filterState.selectedTags.all { selectedTag ->
                    book.tags.contains(selectedTag)
                }
            }
            val matchesShelves = if (filterState.selectedShelves.isEmpty()) true else {
                book.shelfIds.any { it in filterState.selectedShelves }
            }

            matchesSearch && matchesAuthors && matchesTags && matchesShelves
        }
    }


    fun getAvailableFilters(books: List<BookDomainModel>): FilterOptions {
        val allAuthors = books.flatMap { it.authors }.distinct()
        val allTags = books.flatMap { it.tags }.distinct().sorted()
        return FilterOptions(allAuthors, allTags)
    }
}

data class FilterOptions(
    val authors: List<AuthorDomainModel?>,
    val tags: List<String>
)