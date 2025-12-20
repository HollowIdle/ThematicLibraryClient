package com.example.thematiclibraryclient.di

import android.content.Context
import androidx.room.Room
import com.example.thematiclibraryclient.data.local.db.AppDatabase
import com.example.thematiclibraryclient.data.local.dao.BookmarksDao
import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.dao.QuotesDao
import com.example.thematiclibraryclient.data.local.dao.ShelvesDao
import com.example.thematiclibraryclient.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "thematic_library.db"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideBooksDao(database: AppDatabase): BooksDao {
        return database.booksDao()
    }

    @Provides
    @Singleton
    fun provideShelvesDao(database: AppDatabase): ShelvesDao {
        return database.shelvesDao()
    }

    @Provides
    @Singleton
    fun provideQuotesDao(database: AppDatabase): QuotesDao {
        return database.quotesDao()
    }

    @Provides
    @Singleton
    fun provideBookmarksDao(database: AppDatabase): BookmarksDao {
        return database.bookmarksDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}