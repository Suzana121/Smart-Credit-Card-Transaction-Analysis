package com.cardify.app.data.api

import com.cardify.app.data.model.LoginRequest
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.model.RegisterRequest
import com.cardify.app.data.model.ShareItem
import com.cardify.app.data.model.ShareRequest
import com.cardify.app.data.model.Transaction
import com.cardify.app.data.model.StatsResponse
import com.cardify.app.data.model.UpdateResponse
import com.cardify.app.data.model.UpdateUserRequest
import com.cardify.app.data.model.User
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

    @GET("api/shares")
    suspend fun getShares(): Response<List<ShareItem>>

    @POST("api/shares")
    suspend fun postShare(@Body shareRequest: ShareRequest): Response<Map<String, String>>

    @GET("api/stats")
    suspend fun getStats(): Response<StatsResponse>
    @GET("auth/user_details") // הכתובת ב-Flask שלך
    suspend fun getUserProfile(): Response<User>

    @POST("auth/update_account") // הכתובת ב-Flask שלך
    suspend fun updateProfile(@Body request: UpdateUserRequest): Response<UpdateResponse>
}
