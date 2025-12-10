package com.example.thematiclibraryclient.data.local.source

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenLocalDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ITokenLocalDataSource {

    private val TOKEN_KEY = stringPreferencesKey("jwt_token")

    override fun getToken(): Flow<String?> {
        return context.dataStore.data.map {
            preferences -> preferences[TOKEN_KEY]
        }
    }

    override suspend fun saveToken(token: String) {
        Log.d("token saving check"," entered saveToken")
        context.dataStore.edit { settings ->
            settings[TOKEN_KEY] = token
        }
    }

    override suspend fun clearToken() {
        context.dataStore.edit { settings ->
            settings.remove(TOKEN_KEY)
        }
    }

}