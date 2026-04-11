package com.cardify.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminDashboardData(
    val totalUsers: Int = 0,
    val totalTransactions: Int = 0,
    val totalIrregular: Int = 0,
    val irregularRate: Double = 0.0,
    val topSuspiciousBusinesses: List<SuspiciousBusiness> = emptyList(),
    val topUsersWithIrregular: List<UserIrregularCount> = emptyList(),
    val categoryBreakdown: List<CategoryAmount> = emptyList()
)

data class SuspiciousBusiness(val businessName: String, val count: Int)
data class UserIrregularCount(val username: String, val irregularCount: Int)
data class CategoryAmount(val category: String, val amount: Double)

sealed class AdminUiState {
    object Loading : AdminUiState()
    data class Success(val data: AdminDashboardData) : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

class AdminDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

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

    @Suppress("UNCHECKED_CAST")
    private fun parseBusinesses(raw: Any?): List<SuspiciousBusiness> {
        return (raw as? List<Map<String, Any>>)?.map {
            SuspiciousBusiness(
                businessName = it["businessName"]?.toString() ?: "",
                count = it["count"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
            )
        } ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUsers(raw: Any?): List<UserIrregularCount> {
        return (raw as? List<Map<String, Any>>)?.map {
            UserIrregularCount(
                username = it["username"]?.toString() ?: "",
                irregularCount = it["irregularCount"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
            )
        } ?: emptyList()
    }

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