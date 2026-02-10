package com.example.thematiclibraryclient.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import com.example.thematiclibraryclient.data.local.converters.AppTypeConverters
import com.example.thematiclibraryclient.data.local.dao.BookmarksDao
import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.dao.QuotesDao
import com.example.thematiclibraryclient.data.local.dao.ShelvesDao
import com.example.thematiclibraryclient.data.local.dao.UserDao
import com.example.thematiclibraryclient.data.local.entity.BookContentEntity
import com.example.thematiclibraryclient.data.local.entity.BookEntity
import com.example.thematiclibraryclient.data.local.entity.BookmarkEntity
import com.example.thematiclibraryclient.data.local.entity.QuoteEntity
import com.example.thematiclibraryclient.data.local.entity.ShelfEntity
import com.example.thematiclibraryclient.data.local.entity.UserEntity

@Database(
    entities = [
        BookEntity::class,
        BookContentEntity::class,
        ShelfEntity::class,
        QuoteEntity::class,
        BookmarkEntity::class,
        UserEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun booksDao(): BooksDao
    abstract fun shelvesDao(): ShelvesDao
    abstract fun quotesDao(): QuotesDao
    abstract fun bookmarksDao(): BookmarksDao
    abstract fun userDao(): UserDao

    suspend fun clearDatabase() {
        withTransaction {
            userDao().clearUser()
            booksDao().deleteAllBooks()
            booksDao().deleteAllBookContents()
            quotesDao().deleteAllQuotes()
            shelvesDao().clearShelves()
            bookmarksDao().deleteAllBookmarks()
        }
    }
}
