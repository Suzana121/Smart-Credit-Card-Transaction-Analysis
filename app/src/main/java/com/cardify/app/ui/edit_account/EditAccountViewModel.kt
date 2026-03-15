import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.repository.AuthRepository // ודאי שהנתיב נכון אצלך
import kotlinx.coroutines.launch

class EditAccountViewModel(
    // יצירת מופע של ה-Repository (בהמשך כדאי להשתמש ב-Dependency Injection)
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    // --- State Management ---
    // משתני המצב של השדות במסך - מחוברים ישירות ל-UI
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var password by mutableStateOf("")

    // מצבי עזר לממשק המשתמש
    var isLoading by mutableStateOf(true)
    var isUpdating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        fetchCurrentUserDetails()
    }

    /**
     * שליפת נתוני המשתמש מה-DB בעת טעינת המסך
     */
    private fun fetchCurrentUserDetails() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = repository.fetchUserData()
                result.onSuccess { userResponse ->
                    // התיקון כאן: משתמשים ב-userResponse.username (או user.name תלוי במודל שחזר)
                    name = userResponse.name
                    email = userResponse.email
                    phone = userResponse.phone ?: ""
                    password = "" // סיסמה תמיד נשארת ריקה בטעינה מטעמי אבטחה
                }.onFailure { exception ->
                    errorMessage = "Failed to load user data: ${exception.message}"
                }
            } catch (e: Exception) {
                errorMessage = "An unexpected error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * פונקציה שנקראת בלחיצה על כפתור ה-Update
     */
    fun updateAccountDetails(onSuccess: () -> Unit) {
        if (!validateFields()) return

        viewModelScope.launch {
            isUpdating = true
            errorMessage = null

            try {
                // שליחת הנתונים ל-Repository שיעדכן את ה-Flask
                val result = repository.updateUserData(
                    name = name,
                    email = email,
                    phone = phone,
                    pass = password
                )

                result.onSuccess {
                    successMessage = "Account updated successfully!"
                    onSuccess() // ביצוע הניווט חזרה רק לאחר הצלחה
                }.onFailure { exception ->
                    errorMessage = exception.message ?: "Update failed"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error to server"
            } finally {
                isUpdating = false
            }
        }
    }

    /**
     * בדיקת תקינות בסיסית לפני שליחה לשרת
     */
    private fun validateFields(): Boolean {
        if (name.isBlank() || email.isBlank()) {
            errorMessage = "Username and Email cannot be empty"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Invalid email format"
            return false
        }
        // אם הוכנסה סיסמה, אפשר להוסיף בדיקת אורך (למשל מינימום 6 תווים)
        if (password.isNotEmpty() && password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    /**
     * ניקוי הודעות שגיאה
     */
    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}