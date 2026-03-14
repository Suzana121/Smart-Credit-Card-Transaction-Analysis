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