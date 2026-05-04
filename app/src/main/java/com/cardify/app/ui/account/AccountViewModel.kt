package com.cardify.app.ui.account

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class AccountViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var searchedUser by mutableStateOf<UserProfile?>(null)
        private set

    var isSearching by mutableStateOf(false)
        private set

    var searchErrorMessage by mutableStateOf<String?>(null)
        private set

    private val _username = MutableStateFlow(UserSession.username ?: "Guest")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow(UserSession.email ?: "No Email")
    val email: StateFlow<String> = _email

    private val _phone = MutableStateFlow(UserSession.phone ?: "")
    val phone: StateFlow<String> = _phone

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _requests = MutableStateFlow<List<Friend>>(emptyList())
    val requests: StateFlow<List<Friend>> = _requests

    // הוספת State לתוצאות הסנכרון (אנשים שהם פוטנציאל לחברות)
    private val _syncResults = MutableStateFlow<List<Friend>>(emptyList())
    val syncResults: StateFlow<List<Friend>> = _syncResults.asStateFlow()

    init {
        refreshUserData()
        loadFriendsData()
    }

    // תיקון: הפונקציה עכשיו מקבלת אובייקט Friend כדי להתאים לקריאה מה-UI
    fun sendFriendRequest(friend: Friend) {
        viewModelScope.launch {
            try {
                // שימוש בטלפון של החבר שנבחר
                val response = RetrofitClient.apiService.addFriend(FriendRequestData(friend.phone))
                if (response.isSuccessful) {
                    // לאחר שליחה מוצלחת, נסיר אותו מרשימת ההצעות ונטען מחדש חברים
                    _syncResults.value = _syncResults.value.filter { it.phone != friend.phone }
                    loadFriendsData()
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to send friend request", e)
            }
        }
    }

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            // 1. איפוס תוצאות קודמות כדי שלא יופיעו "שאריות"
            _syncResults.value = emptyList()

            val contactNumbers = mutableListOf<String>()
            val contentResolver = context.contentResolver

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    // ניקוי מספר הטלפון מרווחים ומקפים כדי להתאים לפורמט ב-DB
                    val rawNumber = it.getString(numberIndex).replace(Regex("[^0-9+]"), "")
                    if (rawNumber.isNotEmpty()) {
                        contactNumbers.add(rawNumber)
                    }
                }
            }

            if (contactNumbers.isNotEmpty()) {
                try {
                    _isLoading.value = true
                    // שליחת הרשימה לשרת[cite: 5]
                    val syncRequest = SyncContactsRequest(phones = contactNumbers)
                    val response = RetrofitClient.apiService.syncContacts(syncRequest)

                    if (response.isSuccessful) {
                        val matchedUsers = response.body() ?: emptyList()

                        // 2. סינון קפדני: רק מי שחזר מהשרת (Matches) ואינו חבר עדיין[cite: 5]
                        _syncResults.value = matchedUsers.filter { matched ->
                            val isAlreadyFriend = _friends.value.any { it.phone == matched.phone }
                            val isAlreadyRequested = _requests.value.any { it.phone == matched.phone }

                            !isAlreadyFriend && !isAlreadyRequested
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AccountVM", "Failed to sync with server", e)
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    fun loadFriendsData() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    val allLinks = response.body() ?: emptyList()
                    // סינון חברים מאושרים ובקשות שנשלחו (sent_pending)
                    _friends.value = allLinks.filter { it.status == "approved" || it.status == "sent_pending" }
                    // סינון בקשות שהתקבלו ומחכות לאישור
                    _requests.value = allLinks.filter { it.status == "received_pending" }
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Error loading friends", e)
            }
        }
    }

    fun refreshUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchUserData().onSuccess { user ->
                _username.value = user.name
                _email.value = user.email
                _phone.value = user.phone ?: ""
                UserSession.username = user.name
                UserSession.email = user.email
                UserSession.phone = user.phone
            }.onFailure { e ->
                Log.e("AccountVM", "Failed to fetch user data", e)
            }
            _isLoading.value = false
        }
    }

    fun confirmFriendRequest(friend: Friend) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.confirmFriend(FriendActionData(friend.phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to confirm friend", e)
            }
        }
    }

    fun deleteFriendWithOptions(friend: Friend, deleteSentShares: Boolean, deleteReceivedShares: Boolean) {
        viewModelScope.launch {
            try {
                val options = mapOf(
                    "phone" to friend.phone,
                    "delete_sent" to deleteSentShares,
                    "delete_received" to deleteReceivedShares
                )
                val response = RetrofitClient.apiService.deleteFriendWithOptions(options)
                if (response.isSuccessful) {
                    loadFriendsData()
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Smart delete failed", e)
            }
        }
    }

    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        UserSession.clear()
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        onLogoutSuccess()
    }

    fun searchUser(phone: String) {
        searchErrorMessage = null
        searchedUser = null
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty() || !cleanPhone.all { it.isDigit() } || cleanPhone.length < 9 || cleanPhone.length > 10) {
            searchErrorMessage = "Invalid phone number"
            return
        }
        if (cleanPhone == UserSession.phone) {
            searchErrorMessage = "You cannot add yourself"
            return
        }

        viewModelScope.launch {
            isSearching = true
            try {
                val response = RetrofitClient.apiService.searchUserByPhone(cleanPhone)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        searchedUser = UserProfile(id = body.id, name = body.username, phone = body.phone)
                    } else { searchErrorMessage = "User not found" }
                } else { searchErrorMessage = "User not found" }
            } catch (e: Exception) { searchErrorMessage = "Network error" } finally { isSearching = false }
        }
    }

    fun clearSearch() {
        searchedUser = null
        isSearching = false
    }
}