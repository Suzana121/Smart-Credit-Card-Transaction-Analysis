package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableLiveData<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: LiveData<ForgotPasswordState> = _state

    fun sendOtp(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = ForgotPasswordState.Error("Please enter a valid email address")
            return
        }

        _state.value = ForgotPasswordState.Loading

        viewModelScope.launch {
            repository.forgotPassword(email)
                .onSuccess { _state.value = ForgotPasswordState.OtpSent }
                .onFailure { _state.value = ForgotPasswordState.Error(it.message ?: "Failed to send code") }
        }
    }
}

sealed class ForgotPasswordState {
    object Idle    : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object OtpSent : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}