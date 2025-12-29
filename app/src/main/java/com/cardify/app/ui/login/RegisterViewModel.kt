package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.model.RegisterRequest
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

// מצבים של המסך (טעינה, הצלחה, שגיאה)
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    fun register(username: String, email: String, phone: String, password: String) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                // קריאה ל-Repository לביצוע הרישום
                val response = repository.register(username, email, phone, password)

                if (response.isSuccessful) {
                    _registerState.value = RegisterState.Success
                } else {
                    val errorMsg = "Registration failed: ${response.code()}"
                    _registerState.value = RegisterState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Connection error")
            }
        }
    }
}