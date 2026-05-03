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

    suspend fun fetchUserData(): Result<User> {
        return try {
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

    suspend fun updateUserData(name: String, email: String, phone: String, pass: String): Result<Boolean> {
        return try {
            val request = UpdateUserRequest(
                username = name,
                email    = email,
                phone    = phone,
                password = if (pass.isEmpty()) null else pass
            )
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

    // ─────────────────────────────────────────────
    // שחזור סיסמה
    // ─────────────────────────────────────────────

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Code sent")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<String> {
        return try {
            val response = apiService.resetPassword(ResetPasswordRequest(email, otp, newPassword))
            if (response.isSuccessful) {
                Result.success("Password updated successfully")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Reset failed: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
}