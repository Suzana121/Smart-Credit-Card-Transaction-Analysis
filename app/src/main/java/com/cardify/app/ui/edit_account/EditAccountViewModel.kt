package com.cardify.app.ui.edit_account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the edit-account screen.
 *
 * Fetches the current user's profile data on initialisation and exposes mutable Compose
 * state for each editable field. Handles basic client-side validation before sending the
 * update request to the server via [AuthRepository].
 *
 * @param repository The [AuthRepository] used to fetch and update user data.
 */
class EditAccountViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    /** The user's display name; pre-populated from the server on load. */
    var name by mutableStateOf("")
    /** The user's email address; pre-populated from the server on load. */
    var email by mutableStateOf("")
    /** The user's phone number; pre-populated from the server on load. */
    var phone by mutableStateOf("")
    /** New password field; always starts empty for security reasons. */
    var password by mutableStateOf("")

    /** `true` while the initial profile data is being fetched. */
    var isLoading by mutableStateOf(true)
    /** `true` while an update request is in flight. */
    var isUpdating by mutableStateOf(false)
    /** Non-null when validation or a server call produces an error message. */
    var errorMessage by mutableStateOf<String?>(null)
    /** Non-null after a successful update. */
    var successMessage by mutableStateOf<String?>(null)

    init {
        fetchCurrentUserDetails()
    }

    /**
     * Fetches the authenticated user's profile data from the server and populates the
     * editable fields. Sets [isLoading] to `false` when done.
     */
    private fun fetchCurrentUserDetails() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = repository.fetchUserData()
                result.onSuccess { userResponse ->
                    name = userResponse.name
                    email = userResponse.email
                    phone = userResponse.phone ?: ""
                    password = "" // Password is never pre-filled for security
                }.onFailure { exception ->
                    errorMessage = (exception as? Exception)?.toUserMessage()
                        ?: "Failed to load profile. Please try again."
                }
            } catch (e: Exception) {
                errorMessage = e.toUserMessage()
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Validates the current field values and, if valid, sends an update request to the server.
     * Calls [onSuccess] on the main thread if the update succeeds.
     *
     * @param onSuccess Callback invoked after a successful update (typically navigates back).
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
                    onSuccess()
                }.onFailure { exception ->
                    errorMessage = (exception as? Exception)?.toUserMessage()
                        ?: "Update failed. Please try again."
                }
            } catch (e: Exception) {
                errorMessage = e.toUserMessage()
            } finally {
                isUpdating = false
            }
        }
    }

    /**
     * Validates that [name] and [email] are non-blank, [email] is a valid address, and
     * [password] (if provided) is at least 6 characters long.
     *
     * @return `true` if all checks pass; `false` and sets [errorMessage] otherwise.
     */
    private fun validateFields(): Boolean {
        return when {
            name.isBlank() -> {
                errorMessage = "Name cannot be empty."
                false
            }
            email.isBlank() -> {
                errorMessage = "Email cannot be empty."
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                errorMessage = "Please enter a valid email address."
                false
            }
            phone.isNotBlank() && (!phone.all { it.isDigit() } || phone.length !in 9..10) -> {
                errorMessage = "Phone number must be 9–10 digits."
                false
            }
            password.isNotEmpty() && password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters."
                false
            }
            else -> true
        }
    }

    /** Clears both [errorMessage] and [successMessage]. */
    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}