package com.example.thematiclibraryclient.di

import android.content.Context
import com.example.thematiclibraryclient.data.common.NetworkConnectivityObserver
import com.example.thematiclibraryclient.data.common.SessionExpiredNotifier
import com.example.thematiclibraryclient.data.local.dao.BookmarksDao
import com.example.thematiclibraryclient.data.local.dao.BooksDao
import com.example.thematiclibraryclient.data.local.dao.QuotesDao
import com.example.thematiclibraryclient.data.local.dao.ShelvesDao
import com.example.thematiclibraryclient.data.local.dao.UserDao
import com.example.thematiclibraryclient.data.local.source.ITokenLocalDataSource
import com.example.thematiclibraryclient.data.local.source.PaginationCache
import com.example.thematiclibraryclient.data.local.source.TokenLocalDataSourceImpl
import com.example.thematiclibraryclient.data.remote.model.common.AuthInterceptor
import com.example.thematiclibraryclient.data.remote.api.IAuthApi
import com.example.thematiclibraryclient.data.remote.api.IBookmarksApi
import com.example.thematiclibraryclient.data.remote.api.IBooksApi
import com.example.thematiclibraryclient.data.remote.api.INotesApi
import com.example.thematiclibraryclient.data.remote.api.IQuotesApi
import com.example.thematiclibraryclient.data.remote.api.IShelvesApi
import com.example.thematiclibraryclient.data.remote.api.IUserApi
import com.example.thematiclibraryclient.data.repository.AuthRepositoryImpl
import com.example.thematiclibraryclient.data.repository.BookmarksRemoteRepositoryImpl
import com.example.thematiclibraryclient.data.repository.BooksRemoteRepositoryImpl
import com.example.thematiclibraryclient.data.repository.NotesRemoteRepositoryImpl
import com.example.thematiclibraryclient.data.repository.QuotesRemoteRepositoryImpl
import com.example.thematiclibraryclient.data.repository.ShelvesRemoteRepositoryImpl
import com.example.thematiclibraryclient.data.repository.TokenRepositoryImpl
import com.example.thematiclibraryclient.data.repository.UserRemoteRepositoryImpl
import com.example.thematiclibraryclient.domain.common.LocalBookParser
import com.example.thematiclibraryclient.domain.repository.IAuthRepository
import com.example.thematiclibraryclient.domain.repository.IBookmarksRemoteRepository
import com.example.thematiclibraryclient.domain.repository.IBooksRemoteRepository
import com.example.thematiclibraryclient.domain.repository.INotesRemoteRepository
import com.example.thematiclibraryclient.domain.repository.IQuotesRemoteRepository
import com.example.thematiclibraryclient.domain.repository.IShelvesRemoteRepository
import com.example.thematiclibraryclient.domain.repository.ITokenRepository
import com.example.thematiclibraryclient.domain.repository.IUserRemoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://10.0.2.2:8000/"

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenLocalDataSource: ITokenLocalDataSource, sessionExpiredNotifier: SessionExpiredNotifier) : AuthInterceptor {
        return AuthInterceptor(tokenLocalDataSource, sessionExpiredNotifier)
    }

    @Provides
    @Singleton
    fun providePaginationCache(@ApplicationContext context: Context): PaginationCache {
        return PaginationCache(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenLocalDataSource(@ApplicationContext context: Context): ITokenLocalDataSource{
        return TokenLocalDataSourceImpl(context)
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient) : Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    // Api's
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit) : IAuthApi {
        return retrofit.create(IAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBooksApi(retrofit: Retrofit) : IBooksApi{
        return retrofit.create(IBooksApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQuotesApi(retrofit: Retrofit) : IQuotesApi{
        return retrofit.create(IQuotesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBookmarksApi(retrofit: Retrofit): IBookmarksApi {
        return retrofit.create(IBookmarksApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): IUserApi {
        return retrofit.create(IUserApi::class.java)
    }

    @Provides
    @Singleton
    fun providesNotesApi(retrofit: Retrofit): INotesApi {
        return retrofit.create(INotesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideShelvesApi(retrofit: Retrofit): IShelvesApi {
        return retrofit.create(IShelvesApi::class.java)
    }

    // Repositories
    @Provides
    @Singleton
    fun provideAuthRepository(authApi: IAuthApi) : IAuthRepository {
        return AuthRepositoryImpl(authApi)
    }

    @Provides
    @Singleton
    fun provideTokenRepository(localDataSource: ITokenLocalDataSource): ITokenRepository{
        return TokenRepositoryImpl(localDataSource)
    }

    @Provides
    @Singleton
    fun provideBooksRemoteRepository(@ApplicationContext context: Context , booksApi: IBooksApi, shelvesDao: ShelvesDao,  booksDao: BooksDao, localBookParser: LocalBookParser): IBooksRemoteRepository{
        return BooksRemoteRepositoryImpl(booksApi, booksDao, shelvesDao, localBookParser, context)
    }

    @Provides
    @Singleton
    fun provideQuotesRemoteRepository(quotesApi: IQuotesApi, quotesDao: QuotesDao, booksDao: BooksDao, shelvesDao: ShelvesDao): IQuotesRemoteRepository {
        return QuotesRemoteRepositoryImpl(
            quotesApi,
            quotesDao,
            booksDao,
            shelvesDao
        )
    }

    @Provides
    @Singleton
    fun provideBookmarksRemoteRepository(api: IBookmarksApi, bookmarksDao: BookmarksDao, booksDao: BooksDao): IBookmarksRemoteRepository {
        return BookmarksRemoteRepositoryImpl(
            api,
            bookmarksDao,
            booksDao
        )
    }

    @Provides
    @Singleton
    fun provideUserRemoteRepository(api: IUserApi, userDao: UserDao): IUserRemoteRepository {
        return UserRemoteRepositoryImpl(api, userDao)
    }

    @Provides
    @Singleton
    fun provideNotesRemoteRepository(api: INotesApi, quotesDao: QuotesDao): INotesRemoteRepository {
        return NotesRemoteRepositoryImpl(
            api,
            quotesDao
        )
    }

    @Provides
    @Singleton
    fun provideShelvesRemoteRepository(api: IShelvesApi, booksApi: IBooksApi, shelvesDao: ShelvesDao, booksDao: BooksDao): IShelvesRemoteRepository {
        return ShelvesRemoteRepositoryImpl(
            api,
            shelvesDao = shelvesDao,
            booksDao = booksDao,
            booksApi = booksApi
        )
    }


    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(@ApplicationContext context: Context): NetworkConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

}