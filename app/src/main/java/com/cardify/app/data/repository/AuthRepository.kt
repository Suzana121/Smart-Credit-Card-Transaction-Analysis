package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.* // ייבוא של כל המודלים (User, UpdateUserRequest וכו')
import retrofit2.Response

/**
 * Repository for Authentication and User operations
 */
class AuthRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Login user
     */
    suspend fun login(username: String, password: String): Response<LoginResponse> {
        val loginRequest = LoginRequest(username, password)
        return apiService.login(loginRequest)
    }

    /**
     * Register new user
     */
    suspend fun register(username: String, email: String, phone: String, password: String): Response<LoginResponse> {
        val registerRequest = RegisterRequest(username, email, phone, password)
        return apiService.register(registerRequest)
    }

    // --- פונקציות חדשות עבור ה-Edit Account ---

    /**
     * שליפת נתוני המשתמש הנוכחי
     * מחזירה Result כדי שה-ViewModel יוכל לטפל בהצלחה/כישלון בקלות
     */
    suspend fun fetchUserData(): Result<User> {
        return try {
            // קריאה ל-API (צריך להוסיף את getUserProfile ב-ApiService)
            val response = apiService.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch user data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * עדכון נתוני המשתמש
     */
    suspend fun updateUserData(name: String, email: String, phone: String, pass: String): Result<Boolean> {
        return try {
            val request = UpdateUserRequest(
                username = name,
                email = email,
                phone = phone,
                password = if (pass.isEmpty()) null else pass
            )
            // קריאה ל-API (צריך להוסיף את updateProfile ב-ApiService)
            val response = apiService.updateProfile(request)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Update failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}