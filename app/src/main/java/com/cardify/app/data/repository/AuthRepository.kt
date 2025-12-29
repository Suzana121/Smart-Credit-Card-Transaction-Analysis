package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.model.RegisterRequest // ייבוא המודל החדש
import retrofit2.Response

/**
 * Repository for Authentication operations
 */
class AuthRepository {

    private val authApi = RetrofitClient.authApi

    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Response<LoginResponse> {
        val loginRequest = LoginRequest(email, password)
        return authApi.login(loginRequest)
    }

    /**
     * Register new user - מעודכן לשימוש ב-RegisterRequest
     */
    suspend fun register(
        username: String, // שינינו מ-name ל-username כדי להתאים לשרת
        email: String,
        phone: String,
        password: String
    ): Response<LoginResponse> {

        // יצירת האובייקט החדש במקום Map
        val registerRequest = RegisterRequest(
            username = username,
            email = email,
            phone = phone,
            password = password
        )

        // שליחה דרך ה-API
        return authApi.register(registerRequest)
    }
}