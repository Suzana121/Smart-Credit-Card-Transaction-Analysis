package com.cardify.app.data.api

import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.model.RegisterRequest
import com.cardify.app.data.model.Transaction
import com.cardify.app.data.model.StatsResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    // התחברות והרשמה - נשארות אותו דבר (לא צריכות טוקן)
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<LoginResponse>

    // --- פונקציות שדורשות JWT ---
    // שימי לב: מחקנו את ה-@Header כי ה-Interceptor מוסיף אותו לבד!

    @GET("api/transactions")
    suspend fun getTransactions(): Response<List<Transaction>>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>

    @PUT("api/transactions/{id}")
    suspend fun updateTransactionStatus(
        @Path("id") transactionId: String,
        @Body statusUpdate: Map<String, String>
    ): Response<Transaction>

    @GET("api/stats")
    suspend fun getStats(): Response<StatsResponse>
}