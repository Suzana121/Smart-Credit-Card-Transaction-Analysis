package com.cardify.app.ui.account

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.model.*
import com.cardify.app.data.repository.AuthRepository
import com.cardify.app.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * ViewModel for the account screen.
 *
 * Manages the authenticated user's profile data, friends list, incoming friend requests,
 * friend search, and logout. Profile data is fetched from the server on init and is also
 * available reactively through StateFlow properties.
 *
 * @param repository [AuthRepository] instance used for API calls. Defaults to a new instance.
 */
class AccountViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    /** The user found by a phone-number search. `null` when no search has been performed. */
    var searchedUser by mutableStateOf<UserProfile?>(null)
        private set

    /** `true` while a user-search API call is in flight. */
    var isSearching by mutableStateOf(false)
        private set

    /** Error message from the most recent failed search attempt. `null` if no error. */
    var searchErrorMessage by mutableStateOf<String?>(null)
        private set

    private val _username = MutableStateFlow(UserSession.username ?: "Guest")

    /** Display username of the authenticated user. */
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow(UserSession.email ?: "No Email")

    /** Email address of the authenticated user. */
    val email: StateFlow<String> = _email

    private val _phone = MutableStateFlow(UserSession.phone ?: "")

    /** Phone number of the authenticated user. */
    val phone: StateFlow<String> = _phone

    private val _isLoading = MutableStateFlow(false)

    /** `true` while the user profile is being fetched from the server. */
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    /** Confirmed friends and outgoing-pending friend requests. */
    val friends: StateFlow<List<Friend>> = _friends

    private val _requests = MutableStateFlow<List<Friend>>(emptyList())

    /** Incoming friend requests awaiting the current user's confirmation. */
    val requests: StateFlow<List<Friend>> = _requests

    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * One-shot user-facing error message for operations that don't have dedicated UI state.
     * Call [clearError] after displaying to prevent the message from being shown again.
     */
    val errorMessage: StateFlow<String?> = _errorMessage

    /** Clears the current [errorMessage] after it has been shown. */
    fun clearError() { _errorMessage.value = null }

    init {
        refreshUserData()
        loadFriendsData()
    }

    /**
     * Fetches the full friend list and partitions it into [friends] (approved + sent-pending)
     * and [requests] (received-pending).
     */
    fun loadFriendsData() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    val allLinks = response.body() ?: emptyList()
                    _friends.value = allLinks.filter { it.status == "approved" || it.status == "sent_pending" }
                    _requests.value = allLinks.filter { it.status == "received_pending" }
                } else {
                    Log.e("AccountVM", "Load friends HTTP ${response.code()}")
                    if (response.code() == 401) {
                        _errorMessage.value = "Session expired. Please log in again."
                    }
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Error loading friends", e)
                _errorMessage.value = e.toUserMessage()
            }
        }
    }

    /**
     * Re-fetches the current user's profile from the server and updates the local state flows
     * as well as [UserSession].
     */
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

    /**
     * Accepts an incoming friend request from [friend].
     * Refreshes the friends list on success.
     *
     * @param friend The friend whose request to confirm.
     */
    fun confirmFriendRequest(friend: Friend) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.confirmFriend(FriendActionData(friend.phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                } else {
                    Log.e("AccountVM", "Confirm friend HTTP ${response.code()}")
                    _errorMessage.value = "Failed to confirm friend request. Please try again."
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to confirm friend", e)
                _errorMessage.value = e.toUserMessage()
            }
        }
    }

    /**
     * Removes a friend connection with optional cascading deletion of shared transaction records.
     * Refreshes the friends list on success.
     *
     * @param friend The friend to remove.
     * @param deleteSentShares `true` to also delete transactions the current user shared with this friend.
     * @param deleteReceivedShares `true` to also delete transactions this friend shared with the current user.
     */
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
                } else {
                    Log.e("AccountVM", "Delete friend HTTP ${response.code()}")
                    _errorMessage.value = "Failed to remove friend. Please try again."
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Smart delete failed", e)
                _errorMessage.value = e.toUserMessage()
            }
        }
    }

    /**
     * Sends a friend request to the user with the given phone number.
     *
     * @param phone Phone number of the user to add.
     * @param onResult Callback invoked with a human-readable result message for display.
     */
    fun sendFriendRequest(phone: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.addFriend(FriendRequestData(phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                    onResult("Friend request sent successfully!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    when {
                        errorBody?.contains("You cannot add yourself") == true ->
                            onResult("You cannot add yourself as a friend")
                        errorBody?.contains("already exists") == true ->
                            onResult("A friend request to this number already exists")
                        else ->
                            onResult("Error: User not found or operation failed")
                    }
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to send friend request", e)
                onResult("Network error, please try again")
            }
        }
    }

    /**
     * Logs the user out by clearing both the in-memory [UserSession] and the persistent
     * [PreferencesManager] storage, then invokes [onLogoutSuccess] to trigger navigation.
     *
     * @param context Context used to access [PreferencesManager].
     * @param onLogoutSuccess Callback invoked after local data has been cleared.
     */
    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        UserSession.clear()
        PreferencesManager.getInstance(context).clearAll()
        onLogoutSuccess()
    }

    /**
     * Searches for a user by phone number and populates [searchedUser] on success.
     * Validates the input locally before making a network call.
     *
     * @param phone The phone number to search for (9–10 digits).
     */
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
                        searchedUser = UserProfile(id = body.id, name = body.username, phone = body.phone)
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

    /**
     * Clears the current search result and resets the searching state.
     * Typically called when the add-friend bottom sheet is dismissed.
     */
    fun clearSearch() {
        searchedUser = null
        isSearching = false
    }
}
