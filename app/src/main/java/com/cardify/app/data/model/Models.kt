package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Login Request Model
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * Login Response Model
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
 * User Model
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
    val profileImage: String? = null
)

/**
 * מודל לבקשת עדכון חשבון
 */
data class UpdateUserRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("password")
    val password: String? = null // אופציונלי, רק אם המשתמש הזין סיסמה חדשה
)

/**
 * מודל לתשובה מהשרת לאחר עדכון
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
 * Error Response Model
 */
data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String,

    @SerializedName("errors")
    val errors: Map<String, String>? = null
)

data class SharedTransaction(
    @SerializedName("businessName") val businessName: String = "Unknown",
    @SerializedName("amount")       val amount: Double = 0.0,
    @SerializedName("date")         val date: String = "",
    @SerializedName("status")       val status: String = "REGULAR",
    @SerializedName("category")     val category: String = ""
)

data class ShareItem(
    @SerializedName("id")            val id: String = "",
    @SerializedName("sharedBy")      val sharedBy: String = "",
    @SerializedName("sharedWith")    val sharedWith: String = "",
    @SerializedName("transactionId") val transactionId: String = "",
    @SerializedName("date")          val date: String = "",
    @SerializedName("direction")     val direction: String = "outgoing", // "outgoing" | "incoming"
    @SerializedName("transaction")   val transaction: SharedTransaction? = null
)

data class ShareRequest(
    @SerializedName("sharedWith")
    val sharedWith: String,

    @SerializedName("transactionId")
    val transactionId: String
)

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
 * מודל לתשובת הסטטיסטיקה מהשרת
 */
data class StatsResponse(
    @SerializedName("totalSpend") val totalSpend: Double,
    @SerializedName("regularTransactionsCount") val regularTransactionsCount: Int,
    @SerializedName("irregularTransactionsCount") val irregularTransactionsCount: Int,
    @SerializedName("expensesByCategory") val expensesByCategory: List<CategorySpend>,
    @SerializedName("monthlyExpenses") val monthlyExpenses: List<MonthlySpend>
)


data class CategorySpend(
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("percentage") val percentage: Float
)

data class MonthlySpend(
    @SerializedName("month") val month: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("isHighlighted") val isHighlighted: Boolean = false
)

/**
 * מודל לתשובה מהשרת לאחר יצירת שיתוף
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
 * מודל לתשובה מהשרת לאחר העלאת קובץ אקסל
 */
data class UploadResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)
/**
 * מודל לשליחת בקשת חברות (רק טלפון)
 */
data class FriendRequestData(
    @SerializedName("phone")
    val phone: String
)

/**
 * מודל לביצוע פעולה על חבר (כמו אישור או מחיקה)
 */
data class FriendActionData(
    @SerializedName("phone")
    val phone: String
)
data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

data class UpdateLocationRequest(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)

data class Friend(
    @SerializedName("name") val name: String, // שונה מ-username ל-name
    @SerializedName("phone") val phone: String,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("photoUrl") val photoUrl: String? = null
) {
    val photoResource: Int get() = com.cardify.app.R.drawable.user
    val isPending: Boolean get() = status == "sent_pending"
}
data class UserProfile(
    val id: String,
    val name: String,
    val phone: String
)
data class UserSearchResponse(
    val id: String,
    val username: String,
    val phone: String,
    val profile_image: String
)