package com.ttaguchi.weddingtimeline.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private companion object {
        val ACCEPTED_TERMS = booleanPreferencesKey("accepted_terms_v1")
    }

    val acceptedTerms: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[ACCEPTED_TERMS] ?: false
        }

    suspend fun setAcceptedTerms(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ACCEPTED_TERMS] = value
        }
    }
}
