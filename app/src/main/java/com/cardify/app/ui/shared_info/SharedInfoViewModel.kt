package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.ShareItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ShareFilterState(
    val direction: String = "outgoing", // "outgoing" או "incoming"
    val searchQuery: String = ""        // חיפוש לפי מספר טלפון
)

class SharedInfoViewModel : ViewModel() {

    private val _shares = MutableStateFlow<List<ShareItem>>(emptyList())
    private val _filterState = MutableStateFlow(ShareFilterState())
    private val _isLoading = MutableStateFlow(false)

    val filterState = _filterState.asStateFlow()
    val isLoading = _isLoading.asStateFlow()

    // לוגיקת הסינון המעודכנת לפי המודל שלך
    val filteredShares = combine(_shares, _filterState) { shares, filter ->
        shares.filter { share ->
            // 1. סינון לפי כיוון (מול שדה ה-direction במודל)
            val matchesDirection = share.direction == filter.direction

            // 2. סינון לפי חיפוש (בודק את הטלפון של השולח או המקבל בהתאם לכיוון)
            val contactToCompare = if (share.direction == "outgoing") share.sharedWith else share.sharedBy
            val matchesSearch = filter.searchQuery.isEmpty() ||
                    contactToCompare.contains(filter.searchQuery, ignoreCase = true)

            matchesDirection && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshShares()
    }

    fun refreshShares() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getShares()
                if (response.isSuccessful) {
                    _shares.value = response.body() ?: emptyList()
                    Log.d("SharedInfoVM", "Successfully loaded ${_shares.value.size} shares")
                } else {
                    Log.e("SharedInfoVM", "Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SharedInfoVM", "Network error while loading shares", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setDirection(direction: String) {
        _filterState.update { it.copy(direction = direction) }
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }
}