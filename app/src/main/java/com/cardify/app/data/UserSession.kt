package com.cardify.app.data

/**
 * In-memory singleton that holds the currently authenticated user's session data.
 *
 * Populated after a successful login and cleared on logout. Data stored here is
 * lost when the process is killed; persistent storage is handled by [com.cardify.app.utils.PreferencesManager].
 */
object UserSession {
    /** JWT authentication token. Null when no user is logged in. */
    var token: String? = null

    /** Server-assigned unique identifier for the current user. */
    var id: String? = null

    /** Display username of the current user. */
    var username: String? = null

    /** Email address of the current user. */
    var email: String? = null

    /** Phone number of the current user. */
    var phone: String? = null

    /** Role of the current user. Either `"user"` or `"admin"`. */
    var role: String? = null

    /**
     * Returns `true` if the session contains a non-empty authentication token.
     */
    fun isLoggedIn(): Boolean {
        return !token.isNullOrEmpty()
    }

    /**
     * Returns `true` if the current user has the admin role.
     */
    fun isAdmin(): Boolean = role == "admin"

    /**
     * Clears all session fields, effectively signing the user out from memory.
     * Call this alongside [com.cardify.app.utils.PreferencesManager.clearAll] to fully log out.
     */
    fun clear() {
        token = null
        id = null
        username = null
        email = null
        phone = null
        role = null
    }
}
