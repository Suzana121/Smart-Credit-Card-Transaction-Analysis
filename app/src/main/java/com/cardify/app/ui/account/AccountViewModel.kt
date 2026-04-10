package com.cardify.app.ui.account

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*
import com.cardify.app.data.repository.AuthRepository
import com.cardify.app.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        refreshUserData()
        loadFriendsData()
    }

    fun loadFriendsData() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    val allLinks = response.body() ?: emptyList()

                    // חברים מאושרים + בקשות שאני שלחתי וממתינות (sent_pending)
                    _friends.value = allLinks.filter { it.status == "approved" || it.status == "sent_pending" }

                    // בקשות שמחכות לאישור שלי (received_pending)
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

    fun deleteFriendWithOptions(
        friend: Friend,
        deleteSentShares: Boolean,
        deleteReceivedShares: Boolean
    ) {
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

    fun sendFriendRequest(phone: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.addFriend(FriendRequestData(phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                    onResult("בקשת חברות נשלחה בהצלחה!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody?.contains("You cannot add yourself") == true) {
                        onResult("לא ניתן להוסיף את עצמך כחבר")
                    } else if (errorBody?.contains("already exists") == true) {
                        onResult("כבר קיימת בקשת חברות למספר זה")
                    } else {
                        onResult("שגיאה: המשתמש לא נמצא או שהפעולה נכשלה")
                    }
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to send friend request", e)
                onResult("שגיאת תקשורת עם השרת")
            }
        }
    }

    // התיקון הקריטי כאן: שימוש ב-PreferencesManager וב-UserSession.clear()
    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        // 1. ניקוי ה-RAM
        UserSession.clear()

        // 2. ניקוי ה-Storage (הדיסק) בצורה אחידה
        PreferencesManager.getInstance(context).clearAll()

        // 3. חזרה למסך ה-Login
        onLogoutSuccess()
    }

    fun searchUser(phone: String) {
        searchErrorMessage = null
        searchedUser = null

        val cleanPhone = phone.trim()

        if (cleanPhone.isEmpty()) {
            searchErrorMessage = "Please enter a phone number"
            return
        }

        if (!cleanPhone.all { it.isDigit() }) {
            searchErrorMessage = "Phone number must contain only digits"
            return
        }

        if (cleanPhone.length < 9 || cleanPhone.length > 10) {
            searchErrorMessage = "Phone number must be 9-10 digits"
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
                        searchedUser = UserProfile(
                            id = body.id,
                            name = body.username,
                            phone = body.phone
                        )
                    } else {
                        searchErrorMessage = "User not found"
                    }
                } else {
                    searchErrorMessage = "User not found"
                }
            } catch (e: Exception) {
                searchErrorMessage = "Network error, please try again later"
            } finally {
                isSearching = false
            }
        }
    }

    fun clearSearch() {
        searchedUser = null
        isSearching = false
    }
}