package com.cardify.app.data.model

// ─────────────────────────────────────────────
// מודלים לשחזור סיסמה - הוסף לקובץ ה-models הקיים שלך
// ─────────────────────────────────────────────

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email:       String,
    val otp:         String,
    val newPassword: String
)

data class GenericResponse(
    val success: Boolean,
    val message: String?
)