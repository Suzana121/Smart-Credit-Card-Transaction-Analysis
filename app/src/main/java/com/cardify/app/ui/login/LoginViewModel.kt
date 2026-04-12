package com.cardify.app.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the login screen.
 *
 * Validates user input, performs the login API call via [AuthRepository], and exposes
 * the result through [loginState] LiveData so [LoginActivity] can update its UI accordingly.
 * On success the authenticated token and username are written to [UserSession].
 */
class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<LoginState>()

    /** Observable login state. Observed by [LoginActivity] to drive UI updates. */
    val loginState: LiveData<LoginState> = _loginState

    /**
     * Validates the supplied credentials and, if valid, initiates an API login request.
     *
     * Emits [LoginState.Loading] immediately, then either [LoginState.Success] with the server
     * response, or [LoginState.Error] with a human-readable message.
     *
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     */
    fun login(username: String, password: String) {
        val validationError = validateInput(username, password)
        if (validationError != null) {
            _loginState.value = LoginState.Error(validationError)
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "Calling API with username=$username")
                val response = repository.login(username, password)
                Log.d("LoginViewModel", "Response code: ${response.code()}, successful: ${response.isSuccessful}")
                Log.d("LoginViewModel", "Response body: ${response.body()}")
                Log.d("LoginViewModel", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.token != null) {
                        UserSession.token = loginResponse.token
                        UserSession.username = username
                        _loginState.value = LoginState.Success(loginResponse)
                    } else {
                        _loginState.value = LoginState.Error(
                            loginResponse?.message ?: "Incorrect username or password."
                        )
                    }
                } else {
                    _loginState.value = LoginState.Error(
                        when (response.code()) {
                            401  -> "Incorrect username or password."
                            403  -> "Account access denied."
                            404  -> "No account found with that username. Please check your details or create a new account."
                            429  -> "Too many attempts. Please wait and try again."
                            500, 502, 503 -> "Server error. Please try again later."
                            else -> "Login failed (${response.code()}). Please try again."
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception during login: ${e.javaClass.simpleName}: ${e.message}")
                _loginState.value = LoginState.Error(e.toUserMessage())
            }
        }
    }

    /**
     * Validates that username and password meet basic requirements.
     *
     * @param username The username to validate.
     * @param password The password to validate.
     * @return A human-readable error message, or `null` if inputs are valid.
     */
    private fun validateInput(username: String, password: String): String? {
        return when {
            username.isBlank() -> "Username cannot be empty"
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    /**
     * Resets [loginState] to [LoginState.Idle], allowing the screen to clear any
     * error or success banners without triggering another login attempt.
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

/**
 * Sealed class representing all possible states of the login flow.
 */
sealed class LoginState {
    /** Initial state before any login attempt has been made. */
    object Idle : LoginState()

    /** A login request is in progress. */
    object Loading : LoginState()

    /**
     * Login completed successfully.
     *
     * @property response The full server response including the JWT token and user profile.
     */
    data class Success(val response: LoginResponse) : LoginState()

    /**
     * Login failed due to invalid credentials, a server error, or a network issue.
     *
     * @property message Human-readable description of the failure.
     */
    data class Error(val message: String) : LoginState()
}
