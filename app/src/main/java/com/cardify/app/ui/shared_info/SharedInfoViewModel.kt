package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.model.ShareItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Holds the current filter configuration for the shared-info screen.
 *
 * @property direction Which side of the share to show: `"outgoing"` (sent by the user)
 *   or `"incoming"` (received by the user).
 * @property searchQuery Optional search string to filter shares by contact identifier.
 */
data class ShareFilterState(
    val direction: String = "outgoing",
    val searchQuery: String = ""
)

/**
 * ViewModel for the shared-info screen.
 *
 * Fetches all share records from the server and exposes a [filteredShares] flow that
 * re-derives automatically when the [ShareFilterState] changes.
 */
class SharedInfoViewModel : ViewModel() {

    private val _shares = MutableStateFlow<List<ShareItem>>(emptyList())
    private val _filterState = MutableStateFlow(ShareFilterState())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    /** The current filter state (direction + search query). */
    val filterState = _filterState.asStateFlow()

    /** `true` while the share list is being fetched. */
    val isLoading = _isLoading.asStateFlow()

    /**
     * One-shot user-facing error message. Call [clearError] after displaying to prevent
     * the same message from being shown again.
     */
    val errorMessage = _errorMessage.asStateFlow()

    /** Clears the current [errorMessage] after it has been shown to the user. */
    fun clearError() { _errorMessage.value = null }

    /**
     * Filtered list of [ShareItem] objects derived from the full list and the current
     * [filterState]. Filtered by direction first, then by contact search query.
     */
    val filteredShares = combine(_shares, _filterState) { shares, filter ->
        shares.filter { share ->
            val matchesDirection = share.direction == filter.direction
            val contactToCompare = if (share.direction == "outgoing") share.sharedWith else share.sharedBy
            val matchesSearch = filter.searchQuery.isEmpty() ||
                    contactToCompare.contains(filter.searchQuery, ignoreCase = true)
            matchesDirection && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshShares()
    }

    /**
     * Fetches all share records for the authenticated user from the server.
     * Sets [isLoading] to `true` while the request is in flight.
     */
    fun refreshShares() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getShares()
                if (response.isSuccessful) {
                    _shares.value = response.body() ?: emptyList()
                    Log.d("SharedInfoVM", "Loaded ${_shares.value.size} shares")
                } else {
                    Log.e("SharedInfoVM", "Server error: ${response.code()}")
                    _errorMessage.value = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        403  -> "Access denied."
                        500, 502, 503 -> "Server error. Please try again later."
                        else -> "Failed to load shared transactions (${response.code()})."
                    }
                }
            } catch (e: Exception) {
                Log.e("SharedInfoVM", "Network error while loading shares", e)
                _errorMessage.value = e.toUserMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the direction filter and triggers a re-derivation of [filteredShares].
     *
     * @param direction Either `"outgoing"` or `"incoming"`.
     */
    fun setDirection(direction: String) {
        _filterState.update { it.copy(direction = direction) }
    }

    /**
     * Updates the contact search query and triggers a re-derivation of [filteredShares].
     *
     * @param query The search string to filter contacts by.
     */
    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }
}
