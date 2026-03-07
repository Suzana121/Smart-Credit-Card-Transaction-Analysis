package com.cardify.app.data


object UserSession {
    var token: String? = null
    var id: String? = null
    var username: String? = "Guest"
    var email: String? = null

    fun isLoggedIn(): Boolean {
        return token != null
    }

    fun clear() {
        token = null
        id = null
        username = "Guest"
        email = null
    }
}