package com.cardify.app.ui.stats

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.model.StatsResponse
import com.cardify.app.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// מצבי המסך
class StatsViewModel : ViewModel() {
    private val repository = StatsRepository()

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    val availableMonths = listOf("Aug", "Sep", "Oct", "Nov", "Dec", "Jan")
    var selectedMonthIndex by mutableStateOf(availableMonths.size - 1)
        private set

    init {
        loadData()
    }

    fun loadData() {
        val month = availableMonths[selectedMonthIndex]
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            repository.fetchMonthlyStats(month).onSuccess { data ->
                _uiState.value = StatsUiState.Success(data) // כאן data הוא מסוג StatsResponse
            }.onFailure {
                _uiState.value = StatsUiState.Error("Failed to fetch stats")
            }
        }
    }

    fun changeMonth(newIndex: Int) {
        if (newIndex in availableMonths.indices) {
            selectedMonthIndex = newIndex
            loadData()
        }
    }
}

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val data: StatsResponse) : StatsUiState() // שינוי כאן
    data class Error(val message: String) : StatsUiState()
}