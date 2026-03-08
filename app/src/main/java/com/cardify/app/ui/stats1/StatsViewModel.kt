package com.cardify.app.ui.stats1

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

    // קריאה נקייה ללא צורך ב-Context או בטוקן ידני
    fun fetchStats() {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            try {
                // ה-Interceptor ב-RetrofitClient יוסיף את ה-Header באופן אוטומטי
                val response = RetrofitClient.apiService.getStats()

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = StatsUiState.Success(response.body()!!)
                } else {
                    val errorMsg = if (response.code() == 401) "Session expired" else "Error loading data"
                    _uiState.value = StatsUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }
}