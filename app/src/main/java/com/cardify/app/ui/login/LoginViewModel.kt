package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.LoginResponse
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Login Screen
 */
class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(username: String, password: String) {
        val validationError = validateInput(username, password)
        if (validationError != null) {
            _loginState.value = validationError
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val response = repository.login(username, password)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.token != null) {
                        UserSession.token = loginResponse.token
                        UserSession.username = username
                        _loginState.value = LoginState.Success(loginResponse)
                    } else {
                        _loginState.value = LoginState.Error(loginResponse?.message ?: "Login failed")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    android.util.Log.d("LoginDebug", "code=${response.code()} body=$errorBody")
                    val errorState = when {
                        response.code() == 404 -> LoginState.UserNotFound
                        response.code() == 401 -> LoginState.WrongPassword
                        else -> LoginState.Error("Login failed: ${response.code()}")
                    }
                    _loginState.value = errorState
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    private fun validateInput(username: String, password: String): LoginState? {
        return when {
            username.isBlank() && password.isBlank() -> LoginState.BothEmpty
            username.isBlank() -> LoginState.UsernameEmpty
            password.isBlank() -> LoginState.PasswordEmpty
            password.length < 6 -> LoginState.PasswordTooShort
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
    object BothEmpty : LoginState()
    object UsernameEmpty : LoginState()
    object PasswordEmpty : LoginState()
    object PasswordTooShort : LoginState()
    object UserNotFound : LoginState()
    object WrongPassword : LoginState()
    data class Success(val response: LoginResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}