package com.cardify.app.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession // הוספנו את הייבוא הזה!
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Login Screen
 */
class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    // LiveData for UI state
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(username: String, password: String) {
        // Validate input
        val validationError = validateInput(username, password)
        if (validationError != null) {
            _loginState.value = LoginState.Error(validationError)
            return
        }

        // Show loading
        _loginState.value = LoginState.Loading

        // Make API call
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

                        // --- התיקון החשוב כאן! ---
                        // אנחנו שומרים את הטוקן והשם בסשן הגלובלי
                        UserSession.token = loginResponse.token
                        UserSession.username = username
                        // ------------------------

                        _loginState.value = LoginState.Success(loginResponse)
                    } else {
                        _loginState.value = LoginState.Error(
                            loginResponse?.message ?: "Login failed"
                        )
                    }
                } else {
                    _loginState.value = LoginState.Error(
                        "Login failed: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception during login: ${e.javaClass.simpleName}: ${e.message}")
                _loginState.value = LoginState.Error(
                    "Network error: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun validateInput(username: String, password: String): String? {
        return when {
            username.isBlank() -> "Username cannot be empty"
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val response: LoginResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}