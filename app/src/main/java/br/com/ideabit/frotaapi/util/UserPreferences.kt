package br.com.ideabit.frotaapi.util

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val USER_KEY = stringPreferencesKey("user_key")
        val PASSWORD_KEY = stringPreferencesKey("password_key")
    }

    suspend fun saveCredentials(user: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_KEY] = user
            prefs[PASSWORD_KEY] = password
        }
    }

    suspend fun getUser(): String? {
        return context.dataStore.data.map { it[USER_KEY] }.first()
    }

    suspend fun getPassword(): String? {
        return context.dataStore.data.map { it[PASSWORD_KEY] }.first()
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
