package com.cardify.app.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import com.cardify.app.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccountViewModel : ViewModel() {

    private val _username = MutableStateFlow(UserSession.username ?: "Guest")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow(UserSession.email ?: "No Email")
    val email: StateFlow<String> = _email

    // פונקציית התנתקות מלאה
    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        // 1. ניקוי הזיכרון המיידי של האפליקציה (RAM)
        UserSession.clear()

        // 2. ניקוי הזיכרון הקבוע של הטלפון (SharedPreferences)
        // ודאי שהשם "auth_prefs" תואם למה שהגדרת ב-Login
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // 3. ביצוע הניווט חזרה למסך ההתחברות
        onLogoutSuccess()
    }
}