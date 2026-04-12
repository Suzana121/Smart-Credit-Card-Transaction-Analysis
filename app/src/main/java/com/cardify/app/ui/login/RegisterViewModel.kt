package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the registration screen.
 *
 * Submits the new-account form to [AuthRepository] and exposes the result through
 * [registerState] LiveData. On success, the username and email are written to [UserSession]
 * so they are immediately available if the user navigates without re-logging in.
 */
class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)

    /** Observable registration state. Observed by [RegisterActivity] to drive UI updates. */
    val registerState: LiveData<RegisterState> = _registerState

    /**
     * Submits a registration request to the server.
     *
     * Emits [RegisterState.Loading] immediately, then either [RegisterState.Success] or
     * [RegisterState.Error] with a human-readable message.
     *
     * @param username Desired display username.
     * @param email User's email address.
     * @param phone User's phone number.
     * @param password Chosen password.
     */
    fun register(username: String, email: String, phone: String, password: String) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                val response = repository.register(username, email, phone, password)

                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse?.success == true) {
                        UserSession.username = username
                        UserSession.email = email
                        _registerState.value = RegisterState.Success
                    } else {
                        _registerState.value = RegisterState.Error(
                            registerResponse?.message ?: "Registration failed. Please try again."
                        )
                    }
                } else {
                    _registerState.value = RegisterState.Error(
                        when (response.code()) {
                            409  -> "An account with this email or phone number already exists."
                            400  -> "Invalid registration details. Please check your input."
                            404  -> "Registration service not found. Please try again or contact support."
                            500, 502, 503 -> "Server error. Please try again later."
                            else -> "Registration failed. Please try again."
                        }
                    )
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.toUserMessage())
            }
        }
    }
}

/**
 * Sealed class representing all possible states of the registration flow.
 */
sealed class RegisterState {
    /** Initial state before any registration attempt has been made. */
    object Idle : RegisterState()

    /** A registration request is in progress. */
    object Loading : RegisterState()

    /** Registration completed successfully. */
    object Success : RegisterState()

    /**
     * Registration failed.
     *
     * @property message Human-readable description of the failure.
     */
    data class Error(val message: String) : RegisterState()
}
