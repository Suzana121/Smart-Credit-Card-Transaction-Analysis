package com.cardify.app.data.api

import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API Interface for Authentication endpoints
 */
interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("auth/register")
    suspend fun register(@Body registerRequest: Map<String, String>): Response<LoginResponse>
    
    // Add more endpoints as needed
}
