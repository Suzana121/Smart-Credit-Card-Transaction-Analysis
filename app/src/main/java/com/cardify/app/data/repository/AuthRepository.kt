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

    // התיקון: שינינו כאן ל-apiService כדי שיתאים למה ששינינו קודם
    private val apiService = RetrofitClient.apiService

    /**
     * Login user with username and password
     */
    suspend fun login(username: String, password: String): Response<LoginResponse> {
        val loginRequest = LoginRequest(username, password)
        // התיקון: משתמשים ב-apiService
        return apiService.login(loginRequest)
    }

    /**
     * Register new user
     */
    suspend fun register(username: String, email: String, phone: String, password: String): Response<LoginResponse> {
        val registerRequest = RegisterRequest(username, email, phone, password)
        // התיקון: משתמשים ב-apiService
        return apiService.register(registerRequest)
    }
}