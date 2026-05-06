import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class EditAccountViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var name         by mutableStateOf("")
    var email        by mutableStateOf("")
    var phone        by mutableStateOf("")
    var password     by mutableStateOf("")
    var profileImage by mutableStateOf<String?>(null)

    var isLoading    by mutableStateOf(true)
    var isUpdating   by mutableStateOf(false)
    var isUploading  by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var originalEmail = ""
    private var originalPhone = ""

    init { fetchCurrentUserDetails() }

    private fun fetchCurrentUserDetails() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            repository.fetchUserData()
                .onSuccess { user ->
                    name          = user.name
                    email         = user.email
                    phone         = user.phone ?: ""
                    profileImage  = user.profileImage
                    originalEmail = user.email
                    originalPhone = user.phone ?: ""
                }
                .onFailure { errorMessage = "Failed to load user data" }
            isLoading = false
        }
    }

    fun uploadProfileImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            isUploading  = true
            errorMessage = null
            repository.uploadProfileImage(uri, context)
                .onSuccess { url -> profileImage = url }
                .onFailure { errorMessage = "Image upload failed. Please try again." }
            isUploading = false
        }
    }

    fun updateAccountDetails(onSuccess: () -> Unit) {
        if (!validateFields()) return
        viewModelScope.launch {
            isUpdating   = true
            errorMessage = null
            repository.updateUserData(
                name  = name,
                email = email,
                phone = phone,
                pass  = password
            ).onSuccess {
                onSuccess()
            }.onFailure { e ->
                errorMessage = when {
                    e.message?.contains("email", ignoreCase = true) == true ->
                        "This email is already in use by another account"
                    e.message?.contains("phone", ignoreCase = true) == true ->
                        "This phone number is already in use by another account"
                    else -> e.message ?: "Update failed. Please try again."
                }
            }
            isUpdating = false
        }
    }

    private fun validateFields(): Boolean {
        // שם ומייל לא ריקים
        if (name.isBlank() || email.isBlank()) {
            errorMessage = "Name and Email cannot be empty"
            return false
        }

        // פורמט מייל
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Invalid email format"
            return false
        }

        // ולידציית מספר טלפון ישראלי
        if (phone.isNotBlank()) {
            val cleanPhone = phone.trim()
            // מספר ישראלי: 10 ספרות המתחיל ב-05, או 9 ספרות המתחיל ב-5
            val isValidIsraeliPhone = when {
                cleanPhone.startsWith("05") && cleanPhone.length == 10 &&
                        cleanPhone.all { it.isDigit() } -> true
                cleanPhone.startsWith("5") && cleanPhone.length == 9 &&
                        cleanPhone.all { it.isDigit() } -> true
                else -> false
            }
            if (!isValidIsraeliPhone) {
                errorMessage = "Invalid phone number. Must be an Israeli mobile number (e.g. 0521234567)"
                return false
            }
        }

        // ולידציית סיסמה חזקה (רק אם הוזנה)
        if (password.isNotEmpty()) {
            if (password.length < 8) {
                errorMessage = "Password must be at least 8 characters"
                return false
            }
            if (!password.any { it.isUpperCase() }) {
                errorMessage = "Password must contain at least one uppercase letter"
                return false
            }
            if (!password.any { it.isLowerCase() }) {
                errorMessage = "Password must contain at least one lowercase letter"
                return false
            }
            if (!password.any { it.isDigit() }) {
                errorMessage = "Password must contain at least one number"
                return false
            }
            if (!password.any { !it.isLetterOrDigit() }) {
                errorMessage = "Password must contain at least one special character (!@#\$...)"
                return false
            }
        }
        return true
    }

    fun clearError() { errorMessage = null }
}