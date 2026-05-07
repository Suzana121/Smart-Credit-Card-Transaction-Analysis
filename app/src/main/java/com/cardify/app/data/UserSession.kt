package com.cardify.app.data

object UserSession {
    var token:        String? = null
    var id:           String? = null
    var username:     String? = null
    var email:        String? = null
    var phone:        String? = null
    var profileImage: String? = null  // ← חדש: URL תמונת פרופיל

    // alias נוח לשימוש ב-ChatScreen
    val userId: String? get() = id

    fun isLoggedIn(): Boolean = !token.isNullOrEmpty()

    fun clear() {
        token        = null
        id           = null
        username     = null
        email        = null
        phone        = null
        profileImage = null
    }
}