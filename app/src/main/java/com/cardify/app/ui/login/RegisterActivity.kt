package com.cardify.app.ui.login

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.R
import com.cardify.app.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var etUsername:        EditText
    private lateinit var etEmail:           EditText
    private lateinit var etPhone:           EditText
    private lateinit var etPassword:        EditText
    private lateinit var etConfirmPassword: EditText

    private var isPasswordVisible        = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        etUsername        = binding.root.findViewById(R.id.etUsername)
        etEmail           = binding.root.findViewById(R.id.etEmail)
        etPhone           = binding.root.findViewById(R.id.etPhone)
        etPassword        = binding.root.findViewById(R.id.etPassword)
        etConfirmPassword = binding.root.findViewById(R.id.etConfirmPassword)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username        = etUsername.text.toString().trim()
            val email           = etEmail.text.toString().trim()
            val phone           = etPhone.text.toString().trim()
            val password        = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            clearAllErrors()
            var valid = true

            if (username.isEmpty()) {
                setFieldError(etUsername, R.id.tvUsernameError, "Please enter a username")
                valid = false
            }
            if (email.isEmpty()) {
                setFieldError(etEmail, R.id.tvEmailError, "Please enter an email")
                valid = false
            }
            if (password.isEmpty()) {
                setFieldError(etPassword, R.id.tvPasswordError, "Please enter a password")
                valid = false
            } else if (!isPasswordStrong(password)) {
                setFieldError(
                    etPassword, R.id.tvPasswordError,
                    "Password needs 8+ chars, uppercase, number & special character (!@#\$%^&*)"
                )
                valid = false
            }
            if (confirmPassword.isEmpty()) {
                setFieldError(etConfirmPassword, R.id.tvConfirmPasswordError, "Please confirm your password")
                valid = false
            } else if (password.isNotEmpty() && password != confirmPassword) {
                setFieldError(etConfirmPassword, R.id.tvConfirmPasswordError, "Passwords do not match")
                valid = false
            }

            if (valid) {
                viewModel.register(username, email, phone, password)
            }
        }

        // Password visibility toggles
        val ivPasswordToggle = binding.root.findViewById<ImageView>(R.id.ivPasswordToggle)
        ivPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod =
                if (isPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
            etPassword.setSelection(etPassword.text.length)
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

        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterState.Loading -> showLoading()
                is RegisterState.Success -> {
                    hideLoading()
                    com.cardify.app.utils.PreferencesManager.getInstance(this).clearAll()
                    Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is RegisterState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
                else -> hideLoading()
            }
        }
    }

    private fun isPasswordStrong(password: String): Boolean =
        password.length >= 8 &&
        password.any { it.isUpperCase() } &&
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
        clearFieldError(etUsername,        R.id.tvUsernameError)
        clearFieldError(etEmail,           R.id.tvEmailError)
        clearFieldError(etPhone,           R.id.tvPhoneError)
        clearFieldError(etPassword,        R.id.tvPasswordError)
        clearFieldError(etConfirmPassword, R.id.tvConfirmPasswordError)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
    }
}
