package com.cardify.app.ui.stats

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.model.StatsResponse
import com.cardify.app.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the statistics screen.
 *
 * Manages month selection and fetches [StatsResponse] data from [StatsRepository].
 * The selected month index drives [loadData], which is also called automatically on init
 * and whenever the user navigates between months.
 */
class StatsViewModel : ViewModel() {
    private val repository = StatsRepository()

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)

    /** Observable UI state consumed by [StatsScreen]. */
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    /** Ordered list of month abbreviations available for selection in the stats chart. */
    val availableMonths = listOf("Aug", "Sep", "Oct", "Nov", "Dec", "Jan")

    /** Index into [availableMonths] representing the currently displayed month. */
    var selectedMonthIndex by mutableStateOf(availableMonths.size - 1)
        private set

    init {
        loadData()
    }

    /**
     * Fetches statistics for the currently selected month and updates [uiState].
     * Emits [StatsUiState.Loading] while the request is in flight, then either
     * [StatsUiState.Success] or [StatsUiState.Error].
     */
    fun loadData() {
        val month = availableMonths[selectedMonthIndex]
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            repository.fetchMonthlyStats(month).onSuccess { data ->
                _uiState.value = StatsUiState.Success(data)
            }.onFailure { throwable ->
                val msg = (throwable as? Exception)?.toUserMessage()
                    ?: "Failed to load statistics. Please try again."
                _uiState.value = StatsUiState.Error(msg)
            }
        }
    }

    /**
     * Navigates to a different month and reloads the statistics data.
     * No-ops if [newIndex] is out of bounds.
     *
     * @param newIndex The index into [availableMonths] to select.
     */
    fun changeMonth(newIndex: Int) {
        if (newIndex in availableMonths.indices) {
            selectedMonthIndex = newIndex
            loadData()
        }
    }
}

/**
 * Sealed class representing all possible states of the statistics screen.
 */
sealed class StatsUiState {
    /** Data is being fetched from the server. */
    object Loading : StatsUiState()

    /**
     * Data was fetched successfully.
     *
     * @property data The statistics response to display.
     */
    data class Success(val data: StatsResponse) : StatsUiState()

    /**
     * The request failed.
     *
     * @property message Human-readable error description.
     */
    data class Error(val message: String) : StatsUiState()
}
