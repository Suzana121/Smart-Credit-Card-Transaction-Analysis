package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.StatsResponse
import java.lang.Exception

class StatsRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun fetchMonthlyStats(month: String): Result<StatsResponse> {
        return try {
            val response = apiService.getStatsByMonth(month)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}