package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Login Request Model
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
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
