package com.cardify.app.data.repository

import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.StatsResponse
import java.lang.Exception

/**
 * Repository that abstracts spending-statistics API calls.
 */
class StatsRepository {
    private val apiService = RetrofitClient.apiService

    /**
     * Fetches aggregated spending statistics for the given month from the server.
     *
     * @param month Short month abbreviation passed as a path parameter (e.g. `"Jan"`, `"Dec"`).
     * @return [Result.success] with a [StatsResponse] on success, or [Result.failure] with
     *   an exception describing the HTTP error or network failure.
     */
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
