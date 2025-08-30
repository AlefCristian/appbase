package br.com.ideabit.frotaapi.util

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val USER_KEY = stringPreferencesKey("user_key")
        val PASSWORD_KEY = stringPreferencesKey("password_key")
    }

    val userFlow: Flow<String> = context.dataStore.data
        .map { it[USER_KEY] ?: "" }

    val passwordFlow: Flow<String> = context.dataStore.data
        .map { it[PASSWORD_KEY] ?: "" }

    suspend fun saveCredentials(user: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_KEY] = user
            prefs[PASSWORD_KEY] = password
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}