package com.cardify.app.ui.transactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.ActiveFilters
import com.cardify.app.data.model.FilterOptions
import com.cardify.app.data.model.STATIC_CATEGORIES
import com.cardify.app.data.model.Friend
import com.cardify.app.data.model.ShareRequest
import com.cardify.app.data.model.Transaction
import com.cardify.app.data.model.UploadedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class TransactionFilter { ALL, REGULAR, IRREGULAR }

class TransactionsViewModel : ViewModel() {

    private val _allTransactions  = MutableStateFlow<List<Transaction>>(emptyList())
    private val _activeFilter     = MutableStateFlow(TransactionFilter.ALL)
    private val _searchQuery      = MutableStateFlow("")
    private val _isLoading        = MutableStateFlow(false)
    private val _isLoadingMore    = MutableStateFlow(false)
    private val _hasMore          = MutableStateFlow(false)
    private val _friends          = MutableStateFlow<List<Friend>>(emptyList())
    private val _isSendingShare   = MutableStateFlow(false)
    private val _isDownloadingPdf = MutableStateFlow(false)
    private val _pdfUri           = MutableStateFlow<Uri?>(null)
    private val _errorMessage     = MutableStateFlow<String?>(null)
    private val _manualOverrides  = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _uploads          = MutableStateFlow<List<UploadedFile>>(emptyList())
    private val _selectedFileId   = MutableStateFlow<String?>(null)
    private val _isLoadingUploads = MutableStateFlow(false)

    private val _filterOptions    = MutableStateFlow(FilterOptions())
    private val _activeFilters    = MutableStateFlow(ActiveFilters())
    private val _isLoadingFilters = MutableStateFlow(false)

    val activeFilter:      StateFlow<TransactionFilter>   = _activeFilter.asStateFlow()
    val searchQuery:       StateFlow<String>              = _searchQuery.asStateFlow()
    val isLoading:         StateFlow<Boolean>             = _isLoading.asStateFlow()
    val isLoadingMore:     StateFlow<Boolean>             = _isLoadingMore.asStateFlow()
    val hasMore:           StateFlow<Boolean>             = _hasMore.asStateFlow()
    val friends:           StateFlow<List<Friend>>        = _friends.asStateFlow()
    val isSendingShare:    StateFlow<Boolean>             = _isSendingShare.asStateFlow()
    val isDownloadingPdf:  StateFlow<Boolean>             = _isDownloadingPdf.asStateFlow()
    val pdfUri:            StateFlow<Uri?>                = _pdfUri.asStateFlow()
    val errorMessage:      StateFlow<String?>             = _errorMessage.asStateFlow()
    val manualOverrides:   StateFlow<Map<String, String>> = _manualOverrides.asStateFlow()
    val uploads:           StateFlow<List<UploadedFile>>  = _uploads.asStateFlow()
    val selectedFileId:    StateFlow<String?>             = _selectedFileId.asStateFlow()
    val isLoadingUploads:  StateFlow<Boolean>             = _isLoadingUploads.asStateFlow()
    val filterOptions:     StateFlow<FilterOptions>       = _filterOptions.asStateFlow()
    val activeFilters:     StateFlow<ActiveFilters>       = _activeFilters.asStateFlow()
    val isLoadingFilters:  StateFlow<Boolean>             = _isLoadingFilters.asStateFlow()

    // ── סינון בצד הלקוח — עם mapping נכון לקטגוריות ──
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _allTransactions, _searchQuery, _activeFilters
    ) { transactions, query, filters ->
        var result = transactions
        if (query.isNotBlank())
            result = result.filter { it.businessName.contains(query, ignoreCase = true) }
        // ── סינון קטגוריה עם mapping ──
        if (filters.selectedCategories.isNotEmpty())
            result = result.filter { transaction ->
                filters.selectedCategories.any { group ->
                    categoryMatchesGroup(transaction.category, group)
                }
            }
        if (filters.transactionType != null)
            result = result.filter { it.status == filters.transactionType }
        if (filters.minAmount != null)
            result = result.filter { it.amount >= filters.minAmount }
        if (filters.maxAmount != null)
            result = result.filter { it.amount <= filters.maxAmount }
        if (filters.dateFrom != null)
            result = result.filter { it.date >= filters.dateFrom }
        if (filters.dateTo != null)
            result = result.filter { it.date <= filters.dateTo }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchTransactions()
        loadFriends()
        fetchUploads()
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _hasMore.value   = false
            try {
                val response = RetrofitClient.apiService.getTransactions(
                    limit  = 9999,
                    cursor = null,
                    fileId = _selectedFileId.value,
                    status = null
                )
                if (response.isSuccessful) {
                    val transactions = response.body()?.transactions ?: emptyList()
                    _allTransactions.value = transactions
                    if (transactions.isNotEmpty()) {
                        val amounts = transactions.map { it.amount }
                        _filterOptions.value = _filterOptions.value.copy(
                            categories = STATIC_CATEGORIES,
                            minAmount  = amounts.min(),
                            maxAmount  = amounts.max()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionsVM", "Fetch error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchFilterOptions() { /* no-op */ }

    fun applyFilters(filters: ActiveFilters) {
        _activeFilters.value = filters
    }

    fun clearFilters() {
        _activeFilters.value = ActiveFilters()
        _activeFilter.value  = TransactionFilter.ALL
    }

    fun setFilter(filter: TransactionFilter) {
        if (_activeFilter.value == filter) return
        _activeFilter.value  = filter
        _activeFilters.value = _activeFilters.value.copy(
            transactionType = when (filter) {
                TransactionFilter.REGULAR   -> "REGULAR"
                TransactionFilter.IRREGULAR -> "IRREGULAR"
                TransactionFilter.ALL       -> null
            }
        )
    }

    fun fetchUploads() {
        viewModelScope.launch {
            _isLoadingUploads.value = true
            try {
                val response = RetrofitClient.apiService.getUploads()
                if (response.isSuccessful) _uploads.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("TransactionsVM", "Uploads fetch error", e)
            } finally {
                _isLoadingUploads.value = false
            }
        }
    }

    fun selectFile(fileId: String?) {
        if (_selectedFileId.value == fileId) return
        _selectedFileId.value = fileId
        _activeFilters.value  = ActiveFilters()
        _activeFilter.value   = TransactionFilter.ALL
        fetchTransactions()
    }

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) _friends.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("TransactionsVM", "Error loading friends", e)
            }
        }
    }

    fun loadMore() { /* הכל נטען בבת אחת */ }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun clearError()                  { _errorMessage.value = null }
    fun clearPdfUri()                 { _pdfUri.value = null }

    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        val previousList = _allTransactions.value
        _allTransactions.value = previousList.map { t ->
            if (t.id == transactionId) t.copy(status = newStatus) else t
        }
        _manualOverrides.value = _manualOverrides.value + (transactionId to newStatus)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateTransactionStatus(
                    transactionId, mapOf("status" to newStatus)
                )
                if (!response.isSuccessful) {
                    _allTransactions.value = previousList
                    _manualOverrides.value = _manualOverrides.value - transactionId
                }
            } catch (e: Exception) {
                _allTransactions.value = previousList
                _manualOverrides.value = _manualOverrides.value - transactionId
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
                else _errorMessage.value = "Share failed: ${response.code()}"
            } catch (e: Exception) {
                _errorMessage.value = "Network error"
            } finally {
                _isSendingShare.value = false
            }
        }
    }

    fun downloadPdf(context: Context) {
        viewModelScope.launch {
            _isDownloadingPdf.value = true
            try {
                val response = RetrofitClient.apiService.downloadReport(
                    fileId = _selectedFileId.value
                )
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    val file = File(context.cacheDir, "cardify_report.pdf")
                    FileOutputStream(file).use { it.write(body.bytes()) }
                    _pdfUri.value = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                } else {
                    _errorMessage.value = "Failed to download report: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error downloading report"
            } finally {
                _isDownloadingPdf.value = false
            }
        }
    }

    fun sharePdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cardify Transaction Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report via"))
    }

    fun updateProfile(onSuccess: () -> Unit) {
        if (_manualOverrides.value.isEmpty()) return
        viewModelScope.launch {
            try {
                val body     = mapOf<String, Any>("overrides" to _manualOverrides.value)
                val response = RetrofitClient.apiService.updateProfile(body)
                if (response.isSuccessful) {
                    _manualOverrides.value = emptyMap()
                    onSuccess()
                } else {
                    _errorMessage.value = "Profile update failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network error during profile update"
            }
        }
    }
}