package com.cardify.app.ui.stats

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── Data classes לנתוני השרת ───
data class StatsTransaction(
    val id: String,
    val title: String,
    val date: String,
    val amount: Double,
    val category: String = "Other",
    val isIrregular: Boolean = false
)

data class CategoryData(val name: String, val amount: Double)
data class MonthlyExpense(val month: String, val amount: Double)

data class StatsData(
    val totalSpend: Double,
    val regularCount: Int,
    val irregularCount: Int,
    val transactions: List<StatsTransaction>,
    val categories: List<CategoryData>,
    val monthlyExpenses: List<MonthlyExpense>,
    val dataYear: String = ""
)

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val data: StatsData) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

class StatsViewModel : ViewModel() {

    val allMonths = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    // מתחיל בחודש הנוכחי
    var selectedMonthIndex by mutableStateOf(
        java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    )
        private set

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    // חודשים שיש בהם נתונים (מתמלא מהשרת)
    var availableMonths by mutableStateOf<List<String>>(emptyList())
        private set

    init { loadData() }

    fun loadData() {
        val month = allMonths[selectedMonthIndex]
        val year  = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()

        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            try {
                val response = RetrofitClient.apiService.getStats(month = month, year = year)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val data = StatsData(
                        totalSpend    = body["totalSpend"] as? Double ?: 0.0,
                        regularCount  = (body["regularTransactionsCount"] as? Double)?.toInt() ?: 0,
                        irregularCount = (body["irregularTransactionsCount"] as? Double)?.toInt() ?: 0,
                        transactions  = parseTransactions(body["transactions"]),
                        categories    = parseCategories(body["expensesByCategory"]),
                        monthlyExpenses = parseMonthlyExpenses(body["monthlyExpenses"]),
                        dataYear       = body["dataYear"] as? String ?: year
                    )
                    // עדכון רשימת החודשים הזמינים
                    availableMonths = data.monthlyExpenses.map { it.month }
                    _uiState.value = StatsUiState.Success(data)
                } else {
                    _uiState.value = StatsUiState.Error("Failed to load stats")
                }
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun changeMonth(newIndex: Int) {
        if (newIndex in allMonths.indices) {
            selectedMonthIndex = newIndex
            loadData()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTransactions(raw: Any?): List<StatsTransaction> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.map { m ->
            StatsTransaction(
                id          = m["id"] as? String ?: "",
                title       = m["title"] as? String ?: "Unknown",
                date        = m["date"] as? String ?: "",
                amount      = m["amount"] as? Double ?: 0.0,
                category    = m["category"] as? String ?: "Other",
                isIrregular = m["isIrregular"] as? Boolean ?: false
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCategories(raw: Any?): List<CategoryData> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.map { m ->
            CategoryData(
                name   = m["category"] as? String ?: "Other",
                amount = m["amount"] as? Double ?: 0.0
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMonthlyExpenses(raw: Any?): List<MonthlyExpense> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.map { m ->
            MonthlyExpense(
                month  = m["month"] as? String ?: "",
                amount = m["amount"] as? Double ?: 0.0
            )
        }
    }
}