package com.cardify.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession // ייבוא הקובץ שהעלית לי

import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Registration Screen
 * מעודכן עם שמירת נתונים ב-UserSession
 */
class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    fun register(username: String, email: String, phone: String, password: String) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                // שליחת קריאת ההרשמה לשרת
                val response = repository.register(username, email, phone, password)

                if (response.isSuccessful) {
                    val registerResponse = response.body()

                    if (registerResponse?.success == true) {

                        // ====================================================
                        // עדכון ה-UserSession - זה מה שפותר את הבעיה ב-Account!
                        // ====================================================
                        UserSession.username = username
                        UserSession.email = email
                        // אם השרת מחזיר טוקן כבר בהרשמה, אפשר להוסיף:
                        // UserSession.token = registerResponse.token
                        // ====================================================

                        _registerState.value = RegisterState.Success
                    } else {
                        _registerState.value = RegisterState.Error(
                            registerResponse?.message ?: "Registration failed"
                        )
                    }
                } else {
                    _registerState.value = RegisterState.Error("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }
}

// ניהול המצבים של המסך
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}