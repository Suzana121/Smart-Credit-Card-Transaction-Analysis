package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object LinkSent : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableLiveData<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: LiveData<ForgotPasswordState> = _state

    fun sendResetLink(rawEmail: String) {
        val email = rawEmail.trim().lowercase()
        if (email.isBlank()) {
            _state.value = ForgotPasswordState.Error("Please enter your email address")
            return
        }
        _state.value = ForgotPasswordState.Loading

        viewModelScope.launch {
            repository.sendForgotPasswordOtp(email).fold(
                onSuccess = { _state.value = ForgotPasswordState.LinkSent },
                onFailure = { _state.value = ForgotPasswordState.Error(it.message ?: "Failed to send link") }
            )
        }
    }
}
