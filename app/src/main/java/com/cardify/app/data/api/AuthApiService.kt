package com.cardify.app.data.api

import com.cardify.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    // --- Authentication, Profile & Friends (Prefix: /auth) ---

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

    @POST("auth/delete-friend")
    suspend fun deleteFriend(@Body data: FriendActionData): Response<ShareResponse>

    @POST("auth/delete-friend-smart")
    suspend fun deleteFriendWithOptions(
        @Body options: Map<String, @JvmSuppressWildcards Any>
    ): Response<ShareResponse>

    @GET("auth/search_user/{phone}")
    suspend fun searchUserByPhone(@Path("phone") phone: String): Response<UserSearchResponse>

    @POST("auth/update_account")
    suspend fun updateUserDetails(@Body request: UpdateUserRequest): Response<UpdateResponse>

    @POST("auth/update_location")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<UpdateResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<UpdateResponse>

    @GET("auth/stats/{month}")
    suspend fun getStatsByMonth(@Path("month") month: String): Response<StatsResponse>


    // --- Transactions & Shares (Prefix: /api) ---

    @GET("api/transactions")
    suspend fun getTransactions(): Response<List<Transaction>>

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
}