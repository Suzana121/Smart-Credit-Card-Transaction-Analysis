package com.cardify.app.ui.account

import android.content.Context
import android.util.Log
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

                    // חברים מאושרים (סטטוס 'approved')
                    _friends.value = allLinks.filter { it.status == "approved" }

                    // בקשות שמחכות לאישור שלי (הסטטוס החדש מהשרת)
                    _requests.value = allLinks.filter { it.status == "received_pending" }

                    // הערה: אם תרצי להציג גם בקשות שאת שלחת ומחכות,
                    // תוכלי ליצור StateFlow נוסף עבור "sent_pending"
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

    // אישור בקשת חברות
    fun confirmFriendRequest(friend: Friend) {
        viewModelScope.launch {
            try {
                // שימוש ב-FriendActionData כפי שהגדרנו ב-models.kt
                val response = RetrofitClient.apiService.confirmFriend(FriendActionData(friend.phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to confirm friend", e)
            }
        }
    }

    // מחיקת חבר או דחיית בקשה
    fun deleteFriend(friend: Friend) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteFriend(FriendActionData(friend.phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                }
            } catch (e: Exception) {
                Log.e("AccountVM", "Failed to delete friend", e)
            }
        }
    }

    // שליחת בקשת חברות חדשה
    fun sendFriendRequest(phone: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.addFriend(FriendRequestData(phone))
                if (response.isSuccessful) {
                    loadFriendsData()
                    onResult("בקשת חברות נשלחה בהצלחה!")
                } else {
                    // חילוץ הודעת השגיאה הגולמית מהשרת (כמו "You cannot add yourself")
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

    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        UserSession.clear()
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        onLogoutSuccess()
    }
}