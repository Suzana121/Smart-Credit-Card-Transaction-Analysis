package com.cardify.app.ui.stats1 // עדכון השם ל-stats1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.StatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val data: StatsResponse) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

class StatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        fetchStats()
    }

    fun fetchStats() {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            try {
                val response = RetrofitClient.apiService.getStats()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = StatsUiState.Success(response.body()!!)
                } else {
                    _uiState.value = StatsUiState.Error("שגיאה בטעינת הנתונים")
                }
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error("שגיאת רשת: ${e.message}")
            }
        }
    }
}