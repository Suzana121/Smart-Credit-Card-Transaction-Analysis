package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableLiveData<ResetPasswordState>(ResetPasswordState.Idle)
    val state: LiveData<ResetPasswordState> = _state

    fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ) {
        when {
            otp.length != 6 -> {
                _state.value = ResetPasswordState.Error("Please enter the 6-digit code")
                return
            }
            newPassword.length < 6 -> {
                _state.value = ResetPasswordState.Error("Password must be at least 6 characters")
                return
            }
            newPassword != confirmPassword -> {
                _state.value = ResetPasswordState.Error("Passwords do not match")
                return
            }
        }

        _state.value = ResetPasswordState.Loading

        viewModelScope.launch {
            repository.resetPassword(email, otp, newPassword)
                .onSuccess { _state.value = ResetPasswordState.Success }
                .onFailure { _state.value = ResetPasswordState.Error(it.message ?: "Reset failed") }
        }
    }
}

sealed class ResetPasswordState {
    object Idle    : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}