package com.cardify.app.data.api

import com.cardify.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("auth/user_details")
    suspend fun getUserDetails(): Response<User>

    // תוקן: היה /auth/update, עכשיו /auth/update_account
    @POST("auth/update_account")
    suspend fun updateUserDetails(@Body request: UpdateUserRequest): Response<UpdateResponse>

    // חדש: העלאת תמונת פרופיל
    @Multipart
    @POST("auth/upload_profile_image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part
    ): Response<Map<String, String>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericResponse>

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
    suspend fun searchUserByPhone(@Path("phone") phone: String): Response<UserSearchResponse>

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("limit")   limit:  Int     = 20,
        @Query("cursor")  cursor: String? = null,
        @Query("file_id") fileId: String? = null,
        @Query("status")  status: String? = null
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

    @GET("api/stats")
    suspend fun getStats(
        @Query("month") month: String,
        @Query("year")  year:  String
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @GET("api/report")
    @Streaming
    suspend fun downloadReport(@Query("file_id") fileId: String? = null): Response<ResponseBody>

    @POST("api/profile/update")
    suspend fun updateProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, String>>

    // ─── Chat ────────────────────────────────────────────────────────────────

    @GET("api/chats")
    suspend fun getChats(): Response<List<com.cardify.app.data.model.Chat>>

    @POST("api/chats")
    suspend fun createChat(@Body request: com.cardify.app.data.model.CreateChatRequest
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @GET("api/chats/{chatId}/messages")
    suspend fun getMessages(@Path("chatId") chatId: String
    ): Response<List<com.cardify.app.data.model.ChatMessage>>

    @POST("api/chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: com.cardify.app.data.model.SendMessageRequest
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @DELETE("api/chats/{chatId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("chatId")    chatId:    String,
        @Path("messageId") messageId: String
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @POST("api/chats/{chatId}/messages/{messageId}/react")
    suspend fun reactToMessage(
        @Path("chatId")    chatId:    String,
        @Path("messageId") messageId: String,
        @Body request: com.cardify.app.data.model.ReactRequest
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @POST("api/chats/{targetChatId}/messages/forward")
    suspend fun forwardMessage(
        @Path("targetChatId") targetChatId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @GET("api/chats/unread")
    suspend fun getUnreadCount(): Response<com.cardify.app.data.model.UnreadResponse>

    // ─── Global nickname ─────────────────────────────────────────────────────

    @PATCH("api/contacts/{phone}/nickname")
    suspend fun setGlobalNickname(
        @Path("phone") phone: String,
        @Body request: com.cardify.app.data.model.SetNicknameRequest
    ): Response<Map<String, @JvmSuppressWildcards Any>>

    @GET("api/contacts/nicknames")
    suspend fun getAllNicknames(): Response<Map<String, String>>

    // ─── Admin ───────────────────────────────────────────────────────────────

    @GET("auth/admin/dashboard")
    suspend fun getAdminDashboard(): Response<Map<String, Any>>
}