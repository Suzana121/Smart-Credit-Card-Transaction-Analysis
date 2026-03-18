package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.ShareItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val CATEGORY_OPTIONS = listOf("Food", "Health & Fitness", "Transport")
val AMOUNT_MAX = 500f

data class ShareFilterState(
    val direction: String = "outgoing",              // "outgoing" | "incoming"
    val friendName: String = "",
    val status: String = "ALL",                      // "ALL" | "REGULAR" | "IRREGULAR"
    val selectedCategories: Set<String> = emptySet(), // empty = all categories
    val amountRange: ClosedFloatingPointRange<Float> = 0f..AMOUNT_MAX,
    val dateFrom: String = "",
    val dateTo: String = ""
) {
    val hasActiveFilters: Boolean get() =
        friendName.isNotBlank() ||
        status != "ALL" ||
        selectedCategories.isNotEmpty() ||
        amountRange != 0f..AMOUNT_MAX ||
        dateFrom.isNotBlank() ||
        dateTo.isNotBlank()
}

class SharedInfoViewModel : ViewModel() {

    private val _shares = MutableStateFlow<List<ShareItem>>(emptyList())
    private val _filterState = MutableStateFlow(ShareFilterState())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val filterState: StateFlow<ShareFilterState> = _filterState.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val filteredShares: StateFlow<List<ShareItem>> = combine(_shares, _filterState) { shares, f ->
        shares.filter { item ->
            val txn = item.transaction
            val friendField = if (item.direction == "outgoing") item.sharedWith else item.sharedBy
            val amount = txn?.amount?.toFloat() ?: 0f

            item.direction == f.direction &&
            (f.friendName.isBlank() || friendField.contains(f.friendName, ignoreCase = true)) &&
            (f.status == "ALL" || txn?.status == f.status) &&
            (f.selectedCategories.isEmpty() || f.selectedCategories.any {
                txn?.category?.contains(it, ignoreCase = true) == true
            }) &&
            (amount >= f.amountRange.start && amount <= f.amountRange.endInclusive) &&
            (f.dateFrom.isBlank() || (txn?.date ?: item.date) >= f.dateFrom) &&
            (f.dateTo.isBlank()   || (txn?.date ?: item.date) <= f.dateTo)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init { fetchShares() }

    fun fetchShares() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = RetrofitClient.apiService.getShares()
                if (response.isSuccessful) {
                    _shares.value = response.body() ?: emptyList()
                    Log.d("SharedInfoVM", "Loaded ${_shares.value.size} shares")
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("SharedInfoVM", "Fetch failed: HTTP ${response.code()} | $error")
                    _errorMessage.value = "Failed to load shares (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("SharedInfoVM", "Fetch error", e)
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setDirection(direction: String) {
        _filterState.update { it.copy(direction = direction) }
    }

    // Called by the "Apply Filters" button with the pending state from the sheet
    fun applyFilters(pending: ShareFilterState) {
        _filterState.value = pending.copy(direction = _filterState.value.direction)
    }

    fun clearFilters() {
        _filterState.update { it.copy(
            friendName = "",
            status = "ALL",
            selectedCategories = emptySet(),
            amountRange = 0f..AMOUNT_MAX,
            dateFrom = "",
            dateTo = ""
        )}
    }
}
