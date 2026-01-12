package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Login Request Model
 * שינינו כאן כדי לשלוח username במקום email
 */
data class LoginRequest(
    @SerializedName("username") // זה המפתח שהשרת יקבל
    val username: String,       // זה השם בקוד שלנו

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

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String? = null
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