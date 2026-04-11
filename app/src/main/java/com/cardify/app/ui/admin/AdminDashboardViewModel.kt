package com.cardify.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Aggregated system-wide statistics shown on the admin dashboard.
 *
 * @property totalUsers Total number of registered users.
 * @property totalTransactions Total number of transactions across all users.
 * @property totalIrregular Total number of transactions flagged as irregular.
 * @property irregularRate Ratio of irregular to total transactions, expressed as a percentage.
 * @property topSuspiciousBusinesses Ranked list of businesses with the most irregular reports.
 * @property topUsersWithIrregular Ranked list of users with the most suspicious transactions.
 * @property categoryBreakdown Spending totals broken down by category across all users.
 */
data class AdminDashboardData(
    val totalUsers: Int = 0,
    val totalTransactions: Int = 0,
    val totalIrregular: Int = 0,
    val irregularRate: Double = 0.0,
    val topSuspiciousBusinesses: List<SuspiciousBusiness> = emptyList(),
    val topUsersWithIrregular: List<UserIrregularCount> = emptyList(),
    val categoryBreakdown: List<CategoryAmount> = emptyList()
)

/**
 * A business name paired with the number of irregular-transaction reports against it.
 *
 * @property businessName Name of the merchant.
 * @property count Number of transactions flagged as irregular at this merchant.
 */
data class SuspiciousBusiness(val businessName: String, val count: Int)

/**
 * A username paired with their count of suspicious transactions.
 *
 * @property username Display name of the user.
 * @property irregularCount Number of transactions flagged as irregular for this user.
 */
data class UserIrregularCount(val username: String, val irregularCount: Int)

/**
 * Total spending in a single category aggregated across all users.
 *
 * @property category Category name (e.g. `"Food"`, `"Shopping"`).
 * @property amount Total amount spent in this category.
 */
data class CategoryAmount(val category: String, val amount: Double)

/**
 * Sealed class representing all possible states of the admin dashboard screen.
 */
sealed class AdminUiState {
    /** Data is being fetched from the server. */
    object Loading : AdminUiState()

    /**
     * Data was fetched successfully.
     *
     * @property data The parsed dashboard data to display.
     */
    data class Success(val data: AdminDashboardData) : AdminUiState()

    /**
     * The request failed.
     *
     * @property message Human-readable description of the error (e.g. access-denied message).
     */
    data class Error(val message: String) : AdminUiState()
}

/**
 * ViewModel for the admin dashboard screen.
 *
 * Fetches the system-wide dashboard data from the admin endpoint and parses the
 * loosely-typed server response into strongly-typed [AdminDashboardData].
 * Access is restricted to users with the admin role; a 403 response emits an error state.
 */
class AdminDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)

    /** Observable UI state consumed by [AdminDashboardScreen]. */
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    /**
     * Fetches and parses the admin dashboard data.
     * Emits [AdminUiState.Loading] first, then [AdminUiState.Success] or [AdminUiState.Error].
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val response = RetrofitClient.apiService.getAdminDashboard()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.value = AdminUiState.Success(
                            AdminDashboardData(
                                totalUsers = body["totalUsers"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                                totalTransactions = body["totalTransactions"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                                totalIrregular = body["totalIrregular"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                                irregularRate = body["irregularRate"]?.toString()?.toDoubleOrNull() ?: 0.0,
                                topSuspiciousBusinesses = parseBusinesses(body["topSuspiciousBusinesses"]),
                                topUsersWithIrregular = parseUsers(body["topUsersWithIrregular"]),
                                categoryBreakdown = parseCategories(body["categoryBreakdown"])
                            )
                        )
                    }
                } else if (response.code() == 403) {
                    _uiState.value = AdminUiState.Error("Access denied: Admin only")
                } else {
                    _uiState.value = AdminUiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AdminVM", "Error loading dashboard", e)
                _uiState.value = AdminUiState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Parses the raw `topSuspiciousBusinesses` list from the API response map.
     *
     * @param raw The untyped value from the response map.
     * @return A list of [SuspiciousBusiness] objects, or empty if parsing fails.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseBusinesses(raw: Any?): List<SuspiciousBusiness> {
        return (raw as? List<Map<String, Any>>)?.map {
            SuspiciousBusiness(
                businessName = it["businessName"]?.toString() ?: "",
                count = it["count"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
            )
        } ?: emptyList()
    }

    /**
     * Parses the raw `topUsersWithIrregular` list from the API response map.
     *
     * @param raw The untyped value from the response map.
     * @return A list of [UserIrregularCount] objects, or empty if parsing fails.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseUsers(raw: Any?): List<UserIrregularCount> {
        return (raw as? List<Map<String, Any>>)?.map {
            UserIrregularCount(
                username = it["username"]?.toString() ?: "",
                irregularCount = it["irregularCount"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
            )
        } ?: emptyList()
    }

    /**
     * Parses the raw `categoryBreakdown` list from the API response map.
     *
     * @param raw The untyped value from the response map.
     * @return A list of [CategoryAmount] objects, or empty if parsing fails.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseCategories(raw: Any?): List<CategoryAmount> {
        return (raw as? List<Map<String, Any>>)?.map {
            CategoryAmount(
                category = it["category"]?.toString() ?: "",
                amount = it["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
            )
        } ?: emptyList()
    }
}
