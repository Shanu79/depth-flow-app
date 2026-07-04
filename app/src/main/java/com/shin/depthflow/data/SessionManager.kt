package com.shin.depthflow.data

import com.shin.depthflow.util.Logger
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a single instance of DataStore
private val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        val PROFILE_PIC_KEY = stringPreferencesKey("profile_pic_uri")
    }

    // Read the token as a Flow
    val authTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[JWT_TOKEN_KEY]
    }

    // Read the profile pic URI as a Flow
    val profilePicFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_PIC_KEY]
    }

    // Save the token
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = token
        }
    }

    // Save the local profile pic URI
    suspend fun saveProfilePic(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_PIC_KEY] = uri
        }
    }

    // Delete the token (Logout)
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
            preferences.remove(PROFILE_PIC_KEY)
        }
    }
}







