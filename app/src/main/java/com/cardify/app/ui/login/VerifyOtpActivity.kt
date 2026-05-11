package com.cardify.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.R
import com.cardify.app.databinding.ActivityVerifyOtpBinding
import com.google.android.material.snackbar.Snackbar

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding
    private val viewModel: ResetPasswordViewModel by viewModels()
    private lateinit var email: String

    private lateinit var etOtp:             EditText
    private lateinit var etNewPassword:     EditText
    private lateinit var etConfirmPassword: EditText

    private var isNewPasswordVisible     = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra("email") ?: ""
        binding.tvEmailHint.text = "Code sent to: $email"

        etOtp             = binding.root.findViewById(R.id.etOtp)
        etNewPassword     = binding.root.findViewById(R.id.etNewPassword)
        etConfirmPassword = binding.root.findViewById(R.id.etConfirmPassword)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnResetPassword.setOnClickListener {
            val otp             = etOtp.text.toString().trim()
            val newPassword     = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            clearAllErrors()
            var valid = true

            if (otp.isEmpty()) {
                setFieldError(etOtp, R.id.tvOtpError, "Please enter the reset code")
                valid = false
            }
            if (newPassword.isEmpty()) {
                setFieldError(etNewPassword, R.id.tvNewPasswordError, "Please enter a new password")
                valid = false
            } else if (!isPasswordStrong(newPassword)) {
                setFieldError(
                    etNewPassword, R.id.tvNewPasswordError,
                    "Password needs 8+ chars, uppercase, lowercase, number & special character (!@#\$%^&*)"
                )
                valid = false
            }
            if (confirmPassword.isEmpty()) {
                setFieldError(etConfirmPassword, R.id.tvConfirmPasswordError, "Please confirm your password")
                valid = false
            } else if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
                setFieldError(etConfirmPassword, R.id.tvConfirmPasswordError, "Passwords do not match")
                valid = false
            }

            if (valid) {
                viewModel.resetPassword(email, otp, newPassword, confirmPassword)
            }
        }

        // Password visibility toggles
        val ivNewPasswordToggle = binding.root.findViewById<ImageView>(R.id.ivNewPasswordToggle)
        ivNewPasswordToggle.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            etNewPassword.transformationMethod =
                if (isNewPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            ivNewPasswordToggle.setImageResource(
                if (isNewPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
            etNewPassword.setSelection(etNewPassword.text.length)
        }

        val ivConfirmPasswordToggle = binding.root.findViewById<ImageView>(R.id.ivConfirmPasswordToggle)
        ivConfirmPasswordToggle.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.transformationMethod =
                if (isConfirmPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            ivConfirmPasswordToggle.setImageResource(
                if (isConfirmPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        binding.tvResendCode.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ResetPasswordState.Loading -> showLoading()
                is ResetPasswordState.Success -> {
                    hideLoading()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra("password_reset_success", true)
                    startActivity(intent)
                }
                is ResetPasswordState.Error -> {
                    hideLoading()
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> hideLoading()
            }
        }
    }

    // ✅ נוספה בדיקת אות קטנה (isLowerCase) לעקביות עם EditAccountViewModel
    private fun isPasswordStrong(password: String): Boolean =
        password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { it in "!@#\$%^&*" }

    private fun setFieldError(field: EditText, errorViewId: Int, message: String) {
        field.setBackgroundResource(R.drawable.input_field_error)
        val errorView = binding.root.findViewById<TextView>(errorViewId)
        errorView?.text = message
        errorView?.visibility = View.VISIBLE
    }

    private fun clearFieldError(field: EditText, errorViewId: Int) {
        field.setBackgroundResource(R.drawable.input_field_rounded)
        binding.root.findViewById<TextView>(errorViewId)?.visibility = View.GONE
    }

    private fun clearAllErrors() {
        clearFieldError(etOtp,             R.id.tvOtpError)
        clearFieldError(etNewPassword,     R.id.tvNewPasswordError)
        clearFieldError(etConfirmPassword, R.id.tvConfirmPasswordError)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnResetPassword.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnResetPassword.isEnabled = true
    }
}