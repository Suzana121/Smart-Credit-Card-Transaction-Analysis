package com.cardify.app.data

object UserSession {
    var token: String? = null
    var id: String? = null
    var username: String? = null
    var email: String? = null
    var phone: String? = null
    var role: String? = null

    fun isLoggedIn(): Boolean {
        return !token.isNullOrEmpty()
    }
    fun isAdmin(): Boolean = role == "admin"

    fun clear() {
        token = null
        id = null
        username = null
        email = null
        phone = null
        role = null
    }
}