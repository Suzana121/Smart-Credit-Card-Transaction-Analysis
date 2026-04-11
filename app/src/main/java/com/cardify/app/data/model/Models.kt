package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for the login endpoint.
 *
 * @property username The user's username.
 * @property password The user's plaintext password.
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * Response body returned by the login and register endpoints.
 *
 * @property success Whether the server considers the operation successful.
 * @property message Human-readable status message from the server.
 * @property token JWT bearer token to use for authenticated requests. Null on failure.
 * @property user Profile data of the authenticated user. Null on failure.
 */
data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("user")
    val user: User? = null
)

/**
 * Represents a user profile as returned by the server.
 *
 * @property id Unique server-assigned user identifier.
 * @property email User's email address.
 * @property name Display username.
 * @property phone Optional phone number.
 * @property profileImage Optional URL to the user's profile image.
 * @property role Access role, either `"user"` (default) or `"admin"`.
 */
data class User(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val name: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("profile_image")
    val profileImage: String? = null,

    @SerializedName("role")
    val role: String? = "user"
)

/**
 * Request body for updating the authenticated user's account details.
 *
 * @property username New display username.
 * @property email New email address.
 * @property phone New phone number.
 * @property password New password. Pass `null` to leave the existing password unchanged.
 */
data class UpdateUserRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("password")
    val password: String? = null
)

/**
 * Generic server response for update operations (account, location, etc.).
 *
 * @property success Whether the update was applied successfully.
 * @property message Human-readable result message from the server.
 * @property user Updated user profile, if returned by the server.
 */
data class UpdateResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("user")
    val user: User? = null
)

/**
 * Represents a structured error response from the server.
 *
 * @property success Always `false` for error responses.
 * @property message Top-level error description.
 * @property errors Optional map of field-level validation errors, keyed by field name.
 */
data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String,

    @SerializedName("errors")
    val errors: Map<String, String>? = null
)

/**
 * A condensed view of a transaction embedded inside a [ShareItem].
 *
 * @property businessName Merchant name.
 * @property amount Charge amount in NIS.
 * @property date Transaction date string.
 * @property status `"REGULAR"` or `"IRREGULAR"`.
 * @property category Spending category.
 */
data class SharedTransaction(
    @SerializedName("businessName") val businessName: String = "Unknown",
    @SerializedName("amount")       val amount: Double = 0.0,
    @SerializedName("date")         val date: String = "",
    @SerializedName("status")       val status: String = "REGULAR",
    @SerializedName("category")     val category: String = ""
)

/**
 * Represents a transaction share record — a transaction shared between two users.
 *
 * @property id Unique share identifier.
 * @property sharedBy Username or phone of the user who shared the transaction.
 * @property sharedWith Username or phone of the user the transaction was shared with.
 * @property transactionId ID of the associated transaction.
 * @property date Date the share was created.
 * @property direction `"outgoing"` if the current user sent the share, `"incoming"` if received.
 * @property transaction Embedded transaction details, if returned by the server.
 */
data class ShareItem(
    @SerializedName("id")            val id: String = "",
    @SerializedName("sharedBy")      val sharedBy: String = "",
    @SerializedName("sharedWith")    val sharedWith: String = "",
    @SerializedName("transactionId") val transactionId: String = "",
    @SerializedName("date")          val date: String = "",
    @SerializedName("direction")     val direction: String = "outgoing",
    @SerializedName("transaction")   val transaction: SharedTransaction? = null
)

/**
 * Request body for sharing a transaction with a friend.
 *
 * @property sharedWith Phone number of the recipient friend.
 * @property transactionId ID of the transaction to share.
 */
data class ShareRequest(
    @SerializedName("sharedWith")
    val sharedWith: String,

    @SerializedName("transactionId")
    val transactionId: String
)

/**
 * Request body for the registration endpoint.
 *
 * @property username Desired display username.
 * @property email User's email address.
 * @property phone User's phone number.
 * @property password Plaintext password chosen by the user.
 */
data class RegisterRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("password")
    val password: String
)

/**
 * Aggregated spending statistics for a given month.
 *
 * @property totalSpend Total amount spent during the month in NIS.
 * @property regularTransactionsCount Number of transactions classified as regular.
 * @property irregularTransactionsCount Number of transactions classified as suspicious/irregular.
 * @property expensesByCategory Breakdown of spending per category.
 * @property monthlyExpenses Historical monthly totals for the chart.
 */
data class StatsResponse(
    @SerializedName("totalSpend") val totalSpend: Double,
    @SerializedName("regularTransactionsCount") val regularTransactionsCount: Int,
    @SerializedName("irregularTransactionsCount") val irregularTransactionsCount: Int,
    @SerializedName("expensesByCategory") val expensesByCategory: List<CategorySpend>,
    @SerializedName("monthlyExpenses") val monthlyExpenses: List<MonthlySpend>
)

/**
 * Spending total for a single category within a stats response.
 *
 * @property category Category name (e.g. `"Food"`, `"Shopping"`).
 * @property amount Total amount spent in this category.
 * @property percentage Share of this category as a fraction of total spend (0–100).
 */
data class CategorySpend(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("percentage") val percentage: Float
)

/**
 * Total spending for a single month, used to populate historical charts.
 *
 * @property month Short month name (e.g. `"Jan"`, `"Feb"`).
 * @property amount Total amount spent during this month.
 * @property isHighlighted Whether this month should be visually highlighted in the chart.
 */
data class MonthlySpend(
    @SerializedName("month") val month: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("isHighlighted") val isHighlighted: Boolean = false
)

/**
 * Generic server response for share and friend operations.
 *
 * @property success Whether the operation succeeded.
 * @property message Human-readable result message.
 * @property id Optional ID of the newly created resource (e.g. share ID).
 */
data class ShareResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("id")
    val id: String? = null
)

/**
 * Response returned after uploading a CSV/Excel file of transactions.
 *
 * @property message Success message from the server, if the upload was processed.
 * @property error Error description from the server, if processing failed.
 */
data class UploadResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)

/**
 * Request body for sending a friend request, identified by phone number.
 *
 * @property phone Phone number of the user to invite as a friend.
 */
data class FriendRequestData(
    @SerializedName("phone")
    val phone: String
)

/**
 * Request body for performing an action on an existing friend relationship
 * (e.g. confirming or deleting), identified by phone number.
 *
 * @property phone Phone number of the friend to act upon.
 */
data class FriendActionData(
    @SerializedName("phone")
    val phone: String
)

/**
 * Request body for updating the authenticated user's last known location.
 *
 * @property latitude GPS latitude coordinate.
 * @property longitude GPS longitude coordinate.
 */
data class UpdateLocationRequest(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)

/**
 * Represents a friend connection in the user's social graph.
 *
 * @property name Display name of the friend.
 * @property phone Phone number used as the friend's identifier.
 * @property status Relationship status: `"approved"`, `"sent_pending"`, or `"received_pending"`.
 * @property photoUrl Optional URL to the friend's profile photo.
 */
data class Friend(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("photoUrl") val photoUrl: String? = null
) {
    /** Drawable resource ID for this friend's profile photo placeholder. */
    val photoResource: Int get() = com.cardify.app.R.drawable.user

    /** `true` if a friend request has been sent but not yet accepted by the other party. */
    val isPending: Boolean get() = status == "sent_pending"
}

/**
 * A lightweight user profile used internally when searching for friends by phone number.
 *
 * @property id Server-assigned user ID.
 * @property name Display username.
 * @property phone Phone number.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val phone: String
)

/**
 * Response body returned by the user search endpoint.
 *
 * @property id Server-assigned user ID.
 * @property username Display username.
 * @property phone Phone number.
 * @property profile_image URL to the user's profile image.
 */
data class UserSearchResponse(
    val id: String,
    val username: String,
    val phone: String,
    val profile_image: String
)
