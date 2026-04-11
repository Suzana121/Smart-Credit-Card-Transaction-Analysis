package com.cardify.app.ui.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.model.Friend
import com.cardify.app.data.model.ShareRequest
import com.cardify.app.data.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Filter options available on the transactions screen.
 */
enum class TransactionFilter {
    /** Show all transactions regardless of status. */
    ALL,

    /** Show only transactions classified as regular. */
    REGULAR,

    /** Show only transactions classified as suspicious/irregular. */
    IRREGULAR
}

/**
 * ViewModel for the transactions screen.
 *
 * Maintains the full transaction list fetched from the server and exposes a reactive
 * [filteredTransactions] flow that automatically re-derives whenever the active
 * [TransactionFilter] or search query changes.
 */
class TransactionsViewModel : ViewModel() {

    /** Backing store for the complete unfiltered list of transactions. */
    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val _activeFilter = MutableStateFlow(TransactionFilter.ALL)

    /** The currently selected filter chip. */
    val activeFilter: StateFlow<TransactionFilter> = _activeFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /** `true` while the transaction list is being fetched. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    /** The current business-name search query. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    /** Friend list used to populate the share bottom sheet. */
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _isSendingShare = MutableStateFlow(false)

    /** `true` while a share request is in flight. */
    val isSendingShare: StateFlow<Boolean> = _isSendingShare.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * One-shot user-facing error message. Call [clearError] after displaying to prevent
     * the same message from being shown again.
     */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Clears the current [errorMessage] after it has been displayed to the user. */
    fun clearError() { _errorMessage.value = null }

    /**
     * Filtered and searched view of all transactions, derived reactively from
     * [_allTransactions], [_activeFilter], and [_searchQuery].
     */
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _allTransactions, _activeFilter, _searchQuery
    ) { transactions, filter, query ->
        transactions
            .filter { t ->
                when (filter) {
                    TransactionFilter.ALL -> true
                    TransactionFilter.REGULAR -> t.status == "REGULAR"
                    TransactionFilter.IRREGULAR -> t.status == "IRREGULAR"
                }
            }
            .filter { t ->
                if (query.isBlank()) true
                else t.businessName.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchTransactions()
        loadFriends()
    }

    /**
     * Fetches all transactions for the current user from the server.
     * Sets [isLoading] to `true` while the request is in flight.
     */
    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getTransactions()
                if (response.isSuccessful) {
                    _allTransactions.value = response.body() ?: emptyList()
                } else {
                    Log.e("TransactionsViewModel", "Fetch HTTP ${response.code()}")
                    _errorMessage.value = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        403  -> "Access denied."
                        500, 502, 503 -> "Server error. Please try again later."
                        else -> "Failed to load transactions (${response.code()})."
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionsViewModel", "Fetch error", e)
                _errorMessage.value = e.toUserMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads the authenticated user's friend list for the share bottom sheet.
     */
    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    _friends.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TransactionsVM", "Error loading friends", e)
            }
        }
    }

    /**
     * Shares a transaction with a friend.
     *
     * @param friendPhone Phone number of the recipient.
     * @param transactionId ID of the transaction to share.
     * @param onSuccess Callback invoked on success.
     */
    fun sendShare(friendPhone: String, transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSendingShare.value = true
            try {
                val response = RetrofitClient.apiService.postShare(
                    ShareRequest(friendPhone, transactionId)
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("TransactionsVM", "Share failed HTTP ${response.code()}")
                    _errorMessage.value = "Failed to share transaction. Please try again."
                }
            } catch (e: Exception) {
                Log.e("TransactionsVM", "Error sending share", e)
                _errorMessage.value = e.toUserMessage()
            } finally {
                _isSendingShare.value = false
            }
        }
    }

    /**
     * Changes the active transaction filter and triggers a re-derivation of [filteredTransactions].
     *
     * @param filter The new filter to apply.
     */
    fun setFilter(filter: TransactionFilter) {
        _activeFilter.value = filter
    }

    /**
     * Updates the business-name search query and triggers a re-derivation of [filteredTransactions].
     *
     * @param query The search string entered by the user.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Optimistically updates a transaction's status locally and persists the change to the server.
     * Reverts to the previous list if the server call fails.
     *
     * @param transactionId ID of the transaction to update.
     * @param newStatus New status value: `"REGULAR"` or `"IRREGULAR"`.
     */
    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        val previousList = _allTransactions.value
        _allTransactions.value = previousList.map { t ->
            if (t.id == transactionId) t.copy(status = newStatus) else t
        }
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateTransactionStatus(
                    transactionId, mapOf("status" to newStatus)
                )
                if (!response.isSuccessful) {
                    _allTransactions.value = previousList
                    Log.e("TransactionsVM", "Status update failed: ${response.code()}")
                    _errorMessage.value = "Failed to update transaction status. Please try again."
                }
            } catch (e: Exception) {
                _allTransactions.value = previousList
                Log.e("TransactionsVM", "Error updating status", e)
                _errorMessage.value = e.toUserMessage()
            }
        }
    }
}
