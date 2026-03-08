package com.cardify.app.data

object UserSession {
    var token: String? = null
    var id: String? = null
    var username: String? = null
    var email: String? = null

    fun isLoggedIn(): Boolean {
        return !token.isNullOrEmpty()
    }

    fun clear() {
        token = null
        id = null
        username = null
        email = null
    }
}