package com.cardify.app.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
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

    init {
        refreshUserData()
    }

    // משיכת נתונים עדכניים מהשרת (למקרה שהשתנו ב-Edit Account)
    fun refreshUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchUserData().onSuccess { user ->
                _username.value = user.name
                _email.value = user.email
                _phone.value = user.phone ?: ""

                // עדכון ה-Session המקומי בנתונים החדשים
                UserSession.username = user.name
                UserSession.email = user.email
                UserSession.phone = user.phone
            }
            _isLoading.value = false
        }
    }

    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        UserSession.clear()
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        onLogoutSuccess()
    }
}