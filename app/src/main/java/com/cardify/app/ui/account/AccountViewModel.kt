package com.cardify.app.ui.account

import androidx.lifecycle.ViewModel
import com.cardify.app.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccountViewModel : ViewModel() {

    private val _username = MutableStateFlow(UserSession.username ?: "Guest")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow(UserSession.email ?: "No Email")
    val email: StateFlow<String> = _email

    fun logout(onLogoutSuccess: () -> Unit) {
        UserSession.clear()
        onLogoutSuccess()
    }
}