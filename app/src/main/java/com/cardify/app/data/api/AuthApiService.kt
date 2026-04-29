package com.cardify.app.data.api

import com.cardify.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Query

interface AuthApiService {

    // --- Authentication & Friends (Prefix: /auth) ---

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("auth/user_details")
    suspend fun getUserDetails(): Response<User>

    @GET("auth/friends")
    suspend fun getFriends(): Response<List<Friend>>

    @POST("auth/add-friend")
    suspend fun addFriend(@Body data: FriendRequestData): Response<ShareResponse>

    @POST("auth/confirm-friend")
    suspend fun confirmFriend(@Body data: FriendActionData): Response<ShareResponse>

    @POST("auth/delete-friend-smart")
    suspend fun deleteFriendWithOptions(
        @Body options: Map<String, @JvmSuppressWildcards Any>
    ): Response<ShareResponse>

    @GET("auth/search_user/{phone}")
    suspend fun searchUserByPhone(
        @Path("phone") phone: String
    ): Response<UserSearchResponse>

    @POST("auth/update")
    suspend fun updateUserDetails(@Body request: UpdateUserRequest): Response<UpdateResponse>

    // --- Transactions & Shares (Prefix: /api) ---

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("limit")   limit:  Int     = 20,
        @Query("cursor")  cursor: String? = null,
        @Query("file_id") fileId: String? = null
    ): Response<TransactionPage>

    @GET("api/uploads")
    suspend fun getUploads(): Response<List<UploadedFile>>

    @GET("api/shares")
    suspend fun getShares(): Response<List<ShareItem>>

    @POST("api/shares")
    suspend fun postShare(@Body request: ShareRequest): Response<ShareResponse>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResponse>

    @PUT("api/transactions/{id}")
    suspend fun updateTransactionStatus(
        @Path("id") transactionId: String,
        @Body statusRequest: Map<String, String>
    ): Response<Transaction>

    // --- Stats ---
    @GET("api/stats")
    suspend fun getStats(
        @Query("month") month: String,
        @Query("year")  year:  String
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    // --- PDF Report ---
    @GET("api/report")
    @Streaming
    suspend fun downloadReport(
        @Query("file_id") fileId: String? = null
    ): Response<ResponseBody>

    // --- Profile Update ---
    @POST("api/profile/update")
    suspend fun updateProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, String>>

    // --- Admin ---
    @GET("auth/admin/dashboard")
    suspend fun getAdminDashboard(): Response<Map<String, Any>>
}