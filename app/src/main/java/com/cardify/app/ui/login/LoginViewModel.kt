package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    
    /**
     * Perform login
     */
    fun login(email: String, password: String) {
        // Validate input
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _loginState.value = LoginState.Error(validationError)
            return
        }
        
        // Show loading
        _loginState.value = LoginState.Loading
        
        // Make API call
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.token != null) {
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
                _loginState.value = LoginState.Error(
                    "Network error: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Validate input fields
     */
    private fun validateInput(email: String, password: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                "Please enter a valid email"
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }
    
    /**
     * Reset state
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

/**
 * Sealed class for Login states
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val response: LoginResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}
