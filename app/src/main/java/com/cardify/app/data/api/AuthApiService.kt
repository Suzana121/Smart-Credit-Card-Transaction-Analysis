package com.cardify.app.data.api

import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.model.RegisterRequest
import com.cardify.app.data.model.Transaction
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<LoginResponse>

    @GET("api/transactions")
    suspend fun getTransactions(): Response<List<Transaction>>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<Map<String, String>>

    // פונקציה חדשה לעדכון סטטוס עסקה
    @PUT("api/transactions/{id}")
    suspend fun updateTransactionStatus(
        @Path("id") transactionId: String,
        @Body statusUpdate: Map<String, String>
    ): Response<Transaction>
}