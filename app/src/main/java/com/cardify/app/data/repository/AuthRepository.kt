package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
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
     * Register new user
     */
    suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String? = null
    ): Response<LoginResponse> {
        val registerRequest = mutableMapOf(
            "email" to email,
            "password" to password,
            "name" to name
        )
        phone?.let { registerRequest["phone"] = it }
        
        return authApi.register(registerRequest)
    }
}
