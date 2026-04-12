package com.cardify.app.data.api

import com.cardify.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface defining all HTTP endpoints for the Cardify backend.
 *
 * Endpoints are grouped into two prefixes:
 * - `/auth/` — authentication, user profile, friends, and statistics.
 * - `/api/`  — transactions, shares, chat messages, and file uploads.
 *
 * All functions are `suspend` and return a [Response] wrapper so callers can inspect
 * HTTP status codes alongside the deserialized body.
 */
interface AuthApiService {

    // --- Authentication & Profile ---

    /**
     * Authenticates a user and returns a JWT token on success.
     *
     * @param request Credentials containing username and password.
     * @return [LoginResponse] with a bearer token and user profile on success.
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Registers a new user account.
     *
     * @param request New user details including username, email, phone, and password.
     * @return [LoginResponse] mirroring the login response structure.
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    /**
     * Fetches the full profile of the currently authenticated user.
     *
     * @return The authenticated user's [User] object.
     */
    @GET("auth/user_details")
    suspend fun getUserDetails(): Response<User>

    /**
     * Updates the authenticated user's account details.
     *
     * @param request Updated profile fields; pass `null` for password to leave it unchanged.
     * @return [UpdateResponse] indicating success or failure.
     */
    @POST("auth/update_account")
    suspend fun updateUserDetails(@Body request: UpdateUserRequest): Response<UpdateResponse>

    /**
     * Records the user's current GPS location on the server.
     *
     * @param request Latitude and longitude coordinates.
     * @return [UpdateResponse] indicating whether the location was saved.
     */
    @POST("auth/update_location")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<UpdateResponse>

    // --- Friends ---

    /**
     * Returns all friend relationships for the authenticated user, including pending requests.
     *
     * @return List of [Friend] objects with their current relationship status.
     */
    @GET("auth/friends")
    suspend fun getFriends(): Response<List<Friend>>

    /**
     * Sends a friend request to the user identified by the given phone number.
     *
     * @param data Phone number of the target user.
     * @return [ShareResponse] with the result of the operation.
     */
    @POST("auth/add-friend")
    suspend fun addFriend(@Body data: FriendRequestData): Response<ShareResponse>

    /**
     * Confirms an incoming friend request.
     *
     * @param data Phone number of the user whose request to accept.
     * @return [ShareResponse] with the result of the operation.
     */
    @POST("auth/confirm-friend")
    suspend fun confirmFriend(@Body data: FriendActionData): Response<ShareResponse>

    /**
     * Removes an existing friend connection.
     *
     * @param data Phone number of the friend to remove.
     * @return [ShareResponse] with the result of the operation.
     */
    @POST("auth/delete-friend")
    suspend fun deleteFriend(@Body data: FriendActionData): Response<ShareResponse>

    /**
     * Removes a friend and optionally cascades deletion to shared transaction records.
     *
     * @param options Map containing `"phone"`, `"delete_sent"`, and `"delete_received"` keys.
     * @return [ShareResponse] with the result of the operation.
     */
    @POST("auth/delete-friend-smart")
    suspend fun deleteFriendWithOptions(
        @Body options: Map<String, @JvmSuppressWildcards Any>
    ): Response<ShareResponse>

    /**
     * Searches for a user by their phone number.
     *
     * @param phone The phone number to look up.
     * @return [UserSearchResponse] containing the matching user's profile.
     */
    @GET("auth/search_user/{phone}")
    suspend fun searchUserByPhone(@Path("phone") phone: String): Response<UserSearchResponse>

    // --- Statistics ---

    /**
     * Retrieves aggregated spending statistics for the given month.
     *
     * @param month Short month abbreviation (e.g. `"Jan"`, `"Feb"`).
     * @return [StatsResponse] with totals, category breakdown, and irregular counts.
     */
    @GET("auth/stats/{month}")
    suspend fun getStatsByMonth(@Path("month") month: String): Response<StatsResponse>

    // --- Admin ---

    /**
     * Returns a system-wide dashboard summary. Accessible to admin users only (HTTP 403 otherwise).
     *
     * @return A loosely-typed map of dashboard metrics parsed by [com.cardify.app.ui.admin.AdminDashboardViewModel].
     */
    @GET("auth/admin/dashboard")
    suspend fun getAdminDashboard(): Response<Map<String, Any>>

    // --- Transactions & Shares ---

    /**
     * Returns a paginated batch of transactions belonging to the authenticated user.
     *
     * @param limit Maximum number of transactions to return per page.
     * @param lastDocId ID of the last transaction from the previous page, or `null` for the first page.
     * @return List of [com.cardify.app.data.model.Transaction] objects for the requested page.
     */
    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int,
        @Query("last_doc_id") lastDocId: String?
    ): Response<List<com.cardify.app.data.model.Transaction>>

    /**
     * Updates the status (Regular/Irregular) of a single transaction.
     *
     * @param transactionId ID of the transaction to update.
     * @param statusRequest Map containing a `"status"` key with the new value.
     * @return The updated [com.cardify.app.data.model.Transaction].
     */
    @PUT("api/transactions/{id}")
    suspend fun updateTransactionStatus(
        @Path("id") transactionId: String,
        @Body statusRequest: Map<String, String>
    ): Response<com.cardify.app.data.model.Transaction>

    /**
     * Returns all share records (sent and received) for the authenticated user.
     *
     * @return List of [ShareItem] objects.
     */
    @GET("api/shares")
    suspend fun getShares(): Response<List<ShareItem>>

    /**
     * Creates a new share record, sending a transaction to a friend.
     *
     * @param request Contains the recipient's phone number and the transaction ID.
     * @return [ShareResponse] with the new share's ID on success.
     */
    @POST("api/shares")
    suspend fun postShare(@Body request: ShareRequest): Response<ShareResponse>

    /**
     * Retrieves chat messages for a specific share conversation.
     *
     * @param shareId ID of the share whose message thread to load.
     * @return Loosely-typed list of message maps parsed by [com.cardify.app.ui.chat.ChatViewModel].
     */
    @GET("api/messages/{shareId}")
    suspend fun getMessages(@Path("shareId") shareId: String): Response<Any>

    /**
     * Posts a new chat message to a share conversation.
     *
     * @param body Map containing `"shareId"` and `"text"` keys.
     * @return Server acknowledgement.
     */
    @POST("api/messages")
    suspend fun sendMessage(@Body body: Map<String, String>): Response<Any>

    /**
     * Uploads a CSV or Excel file of transactions for server-side parsing and analysis.
     *
     * @param file Multipart file part, typically an `.xlsx` document.
     * @return [UploadResponse] with a success message or error description.
     */
    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResponse>
}
