package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*
import retrofit2.Response
import android.util.Log

/**
 * Repository that abstracts all authentication and user-profile API calls.
 *
 * Each function wraps the corresponding [com.cardify.app.data.api.AuthApiService] call and
 * converts the result into a [Result] or raw [Response] as appropriate.
 */
class AuthRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Attempts to log in with the given credentials.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @return Raw [Response] containing a [LoginResponse] so the caller can inspect HTTP status.
     */
    suspend fun login(username: String, password: String): Response<LoginResponse> {
        val loginRequest = LoginRequest(username, password)
        return apiService.login(loginRequest)
    }

    /**
     * Registers a new user account.
     *
     * @param username Desired display username.
     * @param email User's email address.
     * @param phone User's phone number.
     * @param password Chosen password.
     * @return Raw [Response] containing a [LoginResponse].
     */
    suspend fun register(username: String, email: String, phone: String, password: String): Response<LoginResponse> {
        val registerRequest = RegisterRequest(username, email, phone, password)
        return apiService.register(registerRequest)
    }

    /**
     * Fetches the full profile of the currently authenticated user from the server.
     *
     * @return [Result.success] with a [User] object, or [Result.failure] with an exception
     *   describing the HTTP error or network failure.
     */
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

    /**
     * Sends the device's current GPS coordinates to the server to update the user's location.
     *
     * @param latitude GPS latitude.
     * @param longitude GPS longitude.
     * @return [Result.success] with `true` on success, or [Result.failure] on error.
     */
    suspend fun updateLocation(latitude: Double, longitude: Double): Result<Boolean> {
        return try {
            val response = apiService.updateLocation(UpdateLocationRequest(latitude, longitude))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Location update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "updateLocation error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates the authenticated user's account details on the server.
     *
     * @param name New display name.
     * @param email New email address.
     * @param phone New phone number.
     * @param pass New password. Pass an empty string to leave the existing password unchanged.
     * @return [Result.success] with `true` on success, or [Result.failure] on error.
     */
    suspend fun updateUserData(name: String, email: String, phone: String, pass: String): Result<Boolean> {
        return try {
            val request = UpdateUserRequest(
                username = name,
                email = email,
                phone = phone,
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
}
