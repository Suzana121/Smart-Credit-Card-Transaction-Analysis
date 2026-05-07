package com.cardify.app.ui.account

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var searchedUser        by mutableStateOf<UserProfile?>(null); private set
    var isSearching         by mutableStateOf(false);               private set
    var searchErrorMessage  by mutableStateOf<String?>(null);       private set

    private val _username = MutableStateFlow(UserSession.username ?: "Guest")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow(UserSession.email ?: "No Email")
    val email: StateFlow<String> = _email

    private val _phone = MutableStateFlow(UserSession.phone ?: "")
    val phone: StateFlow<String> = _phone

    // ← חדש: תמונת פרופיל
    private val _profileImage = MutableStateFlow(UserSession.profileImage ?: "")
    val profileImage: StateFlow<String> = _profileImage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _requests = MutableStateFlow<List<Friend>>(emptyList())
    val requests: StateFlow<List<Friend>> = _requests

    private val _nicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val nicknames: StateFlow<Map<String, String>> = _nicknames

    init {
        refreshUserData()
        loadFriendsData()
        loadNicknames()
    }

    // ─── כינויים ──────────────────────────────────────────────────────────────

    fun loadNicknames() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllNicknames()
                if (response.isSuccessful) {
                    _nicknames.value = response.body() ?: emptyMap()
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "loadNicknames error", e)
            }
        }
    }

    fun setNickname(
        phone:     String,
        nickname:  String,
        onSuccess: () -> Unit = {},
        onError:   (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.setGlobalNickname(
                    phone, SetNicknameRequest(nickname)
                )
                if (response.isSuccessful) {
                    _nicknames.value = if (nickname.isBlank()) {
                        _nicknames.value - phone
                    } else {
                        _nicknames.value + (phone to nickname)
                    }
                    onSuccess()
                } else {
                    onError(response.errorBody()?.string() ?: "Error")
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "setNickname error", e)
                onError(e.message ?: "Error")
            }
        }
    }

    fun displayNameFor(friend: Friend): String =
        _nicknames.value[friend.phone]?.takeIf { it.isNotBlank() } ?: friend.name

    // ─── חברים ────────────────────────────────────────────────────────────────

    fun loadFriendsData() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    val allLinks = response.body() ?: emptyList()
                    _friends.value  = allLinks.filter { it.status == "approved" || it.status == "sent_pending" }
                    _requests.value = allLinks.filter { it.status == "received_pending" }
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Error loading friends", e)
            }
        }
    }

    fun confirmFriendRequest(friend: Friend) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.confirmFriend(FriendActionData(friend.phone))
                if (response.isSuccessful) loadFriendsData()
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to confirm friend", e)
            }
        }
    }

    fun deleteFriendWithOptions(friend: Friend, deleteSentShares: Boolean, deleteReceivedShares: Boolean) {
        viewModelScope.launch {
            try {
                val options = mapOf(
                    "phone"           to friend.phone,
                    "delete_sent"     to deleteSentShares,
                    "delete_received" to deleteReceivedShares
                )
                val response = RetrofitClient.apiService.deleteFriendWithOptions(options)
                if (response.isSuccessful) loadFriendsData()
            } catch (e: Exception) {
                Log.e("AccountVM", "Smart delete failed", e)
            }
        }
    }

    // ─── פרופיל ───────────────────────────────────────────────────────────────

    fun refreshUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchUserData().onSuccess { user ->
                _username.value     = user.name
                _email.value        = user.email
                _phone.value        = user.phone ?: ""
                _profileImage.value = user.profileImage ?: ""  // ← עדכון תמונה

                // שמירה ב-UserSession כדי שיהיה זמין בכל המסכים
                UserSession.username     = user.name
                UserSession.email        = user.email
                UserSession.phone        = user.phone
                UserSession.profileImage = user.profileImage
            }.onFailure { e ->
                Log.e("AccountVM", "Failed to fetch user data", e)
            }
            _isLoading.value = false
        }
    }

    fun sendFriendRequest(phone: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.addFriend(FriendRequestData(phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                    onResult("Friend request sent!")
                } else {
                    val err = response.errorBody()?.string() ?: ""
                    onResult(when {
                        err.contains("yourself")       -> "You cannot add yourself"
                        err.contains("already exists") -> "Request already exists"
                        else                           -> "User not found"
                    })
                }
            } catch (e: Exception) {
                onResult("Network error")
            }
        }
    }

    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        UserSession.clear()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        onLogoutSuccess()
    }

    fun searchUser(phone: String) {
        searchErrorMessage = null; searchedUser = null
        val clean = phone.trim()
        if (clean.isEmpty())                        { searchErrorMessage = "Please enter a phone number"; return }
        if (!clean.all { it.isDigit() })            { searchErrorMessage = "Digits only"; return }
        if (clean.length < 9 || clean.length > 10) { searchErrorMessage = "9-10 digits required"; return }
        if (clean == UserSession.phone)             { searchErrorMessage = "You cannot add yourself"; return }

        viewModelScope.launch {
            isSearching = true
            try {
                val response = RetrofitClient.apiService.searchUserByPhone(clean)
                if (response.isSuccessful) {
                    val body = response.body()
                    searchedUser = if (body != null)
                        UserProfile(id = body.id, name = body.username, phone = body.phone)
                    else null
                    if (searchedUser == null) searchErrorMessage = "User not found"
                } else searchErrorMessage = "User not found"
            } catch (e: Exception) { searchErrorMessage = "Network error" }
            finally { isSearching = false }
        }
    }

    fun clearSearch() { searchedUser = null; isSearching = false }
}