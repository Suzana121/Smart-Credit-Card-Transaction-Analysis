package com.cardify.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cardify.app.R
import com.cardify.app.databinding.ActivityLoginBinding
import com.cardify.app.utils.PreferencesManager
import com.google.android.material.snackbar.Snackbar
import com.cardify.app.MainActivity
import com.cardify.app.data.UserSession

/**
 * Login Activity - מעודכן עם קישור למסך הרשמה
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Initialize PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)

        // Check if already logged in
        if (preferencesManager.isLoggedIn()) {
            navigateToHome()
            return
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // כפתור התחברות
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                showError("Please enter email and password")
            }
        }

        // --- התיקון: מעבר למסך הרשמה ---
        // ודאי שה-ID ב-XML הוא tvSignUp או שנו אותו בהתאם
        binding.btnRegister?.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot Password - Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle -> hideLoading()
                is LoginState.Loading -> showLoading()
                is LoginState.Success -> {
                    hideLoading()
                    handleLoginSuccess(state)
                }
                is LoginState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
            }
        }
    }

    private fun handleLoginSuccess(state: LoginState.Success) {
        val response = state.response

        // 1. שמירה בדיסק (כמו שהיה לך)
        response.token?.let { token ->
            preferencesManager.saveToken(token)
        }

        response.user?.let { user ->
            preferencesManager.saveUserData(
                userId = user.id,
                email = user.email,
                name = user.name ?: "User"
            )
        }

        // ====================================================
        // 2. התיקון: שמירה בזיכרון המיידי עבור מסך הבית!
        // ====================================================
        UserSession.token = response.token
        // כאן אנחנו אומרים: קח את השם, אם אין קח את האימייל, אם אין כתוב User
        UserSession.username = response.user?.name ?: response.user?.email ?: "User"
        UserSession.email = response.user?.email
        UserSession.id = response.user?.id
        // ====================================================

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
        navigateToHome()
    }
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.isEnabled = true
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.status_error))
            .setTextColor(getColor(R.color.text_white))
            .show()
    }
}