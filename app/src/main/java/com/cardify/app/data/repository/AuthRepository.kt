package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*
import retrofit2.Response

class AuthRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun login(username: String, password: String): Response<LoginResponse> {
        val loginRequest = LoginRequest(username, password)
        return apiService.login(loginRequest)
    }

    suspend fun register(username: String, email: String, phone: String, password: String): Response<LoginResponse> {
        val registerRequest = RegisterRequest(username, email, phone, password)
        return apiService.register(registerRequest)
    }

    /**
     * שליפת נתוני המשתמש הנוכחי
     */
    suspend fun fetchUserData(): Result<User> {
        return try {
            // שינוי שם הפונקציה ל-getUserDetails (כמו ב-ApiService)
            val response = apiService.getUserDetails()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch user data: ${response.code()}"))
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

            // ודאי שב-AuthApiService קראת לפונקציית העדכון בשם הזה
            // אם לא הוספת אותה ל-Interface, כדאי להוסיף:
            // @POST("auth/update") suspend fun updateUserDetails(@Body request: UpdateUserRequest): Response<UpdateResponse>
            val response = apiService.updateUserDetails(request)

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