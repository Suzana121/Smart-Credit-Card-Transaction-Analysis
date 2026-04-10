package com.cardify.app.ui.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
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

enum class TransactionFilter { ALL, REGULAR, IRREGULAR }

class TransactionsViewModel : ViewModel() {

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val _activeFilter = MutableStateFlow(TransactionFilter.ALL)
    val activeFilter: StateFlow<TransactionFilter> = _activeFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _isSendingShare = MutableStateFlow(false)
    val isSendingShare: StateFlow<Boolean> = _isSendingShare.asStateFlow()

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

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getTransactions()
                if (response.isSuccessful) {
                    _allTransactions.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TransactionsViewModel", "Fetch error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun sendShare(friendPhone: String, transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSendingShare.value = true
            try {
                val response = RetrofitClient.apiService.postShare(
                    ShareRequest(friendPhone, transactionId)
                )
                if (response.isSuccessful) onSuccess()
            } catch (e: Exception) {
                Log.e("TransactionsVM", "Error sending share", e)
            } finally {
                _isSendingShare.value = false
            }
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

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
                    Log.e("TransactionsVM", "Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _allTransactions.value = previousList
                Log.e("TransactionsVM", "Error updating status", e)
            }
        }
    }
}