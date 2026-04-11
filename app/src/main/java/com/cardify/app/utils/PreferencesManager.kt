package com.cardify.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages secure, persistent storage of authentication tokens and user profile data.
 *
 * Uses [EncryptedSharedPreferences] backed by AES-256-GCM to protect stored values at rest.
 * Obtain the singleton instance via [getInstance].
 *
 * @constructor Private — use [getInstance] to obtain the application-scoped singleton.
 * @param context Application context used to create the encrypted preferences file.
 */
class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "cardify_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        @Volatile
        private var instance: PreferencesManager? = null

        /**
         * Returns the application-scoped singleton instance, creating it if necessary.
         * Thread-safe via double-checked locking.
         *
         * @param context Any context; the application context is used internally.
         * @return The singleton [PreferencesManager] instance.
         */
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Persists the JWT authentication token to encrypted storage.
     *
     * @param token JWT bearer token received from the server.
     */
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Retrieves the stored JWT authentication token.
     *
     * @return The stored token, or `null` if no token has been saved.
     */
    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)

    /**
     * Persists the authenticated user's core profile data and marks the session as logged in.
     *
     * @param userId Server-assigned user ID.
     * @param email User's email address.
     * @param name User's display name.
     */
    fun saveUserData(userId: String, email: String, name: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Retrieves the stored user ID.
     *
     * @return The stored user ID, or `null` if not saved.
     */
    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)

    /**
     * Retrieves the stored user email address.
     *
     * @return The stored email, or `null` if not saved.
     */
    fun getUserEmail(): String? = sharedPreferences.getString(KEY_USER_EMAIL, null)

    /**
     * Retrieves the stored display username.
     *
     * @return The stored username, or `null` if not saved.
     */
    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)

    /**
     * Returns `true` if a previous session was marked as logged in.
     */
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Removes all data from encrypted storage, effectively logging the user out on disk.
     * Should be called alongside [com.cardify.app.data.UserSession.clear] to fully sign out.
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
