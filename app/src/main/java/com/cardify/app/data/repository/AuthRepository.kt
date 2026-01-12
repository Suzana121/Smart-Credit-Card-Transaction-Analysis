package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.model.RegisterRequest
import retrofit2.Response

/**
 * Repository for Authentication operations
 */
class AuthRepository {

    private val authApi = RetrofitClient.authApi

    /**
     * Login user with username and password
     * שינינו כאן את הפרמטר מ-email ל-username
     */
    suspend fun login(username: String, password: String): Response<LoginResponse> {
        // כאן אנחנו יוצרים את בקשת ההתחברות עם השם משתמש החדש
        val loginRequest = LoginRequest(username, password)
        return authApi.login(loginRequest)
    }

    /**
     * Register new user
     */
    suspend fun register(
        username: String,
        email: String,
        phone: String,
        password: String
    ): Response<LoginResponse> {

        val registerRequest = RegisterRequest(
            username = username,
            email = email,
            phone = phone,
            password = password
        )

        return authApi.register(registerRequest)
    }
}