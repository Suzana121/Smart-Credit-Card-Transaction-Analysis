package com.cardify.app

/**
 * Global network configuration constants for the Cardify backend server.
 */
object NetworkConfig {
    /** Base URL of the backend server, including protocol and port. */
   // const val BASE_URL = "http://192.168.33.16:5001/"
    const val BASE_URL = "http://192.168.33.11:5001/"//מכשיר פיזי
    /** Full URL for the user registration endpoint. */
    const val REGISTER_URL = "${BASE_URL}auth/register"
}
