package com.cardify.app.data.api

import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.model.RegisterRequest // ייבוא של המודל החדש
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API Interface for Authentication endpoints
 */
interface AuthApiService {
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<LoginResponse>
}