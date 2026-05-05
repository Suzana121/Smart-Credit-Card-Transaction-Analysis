package com.cardify.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cardify.app.MainActivity
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // בדיקה אם המשתמש כבר מחובר
        val prefs      = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)

        if (savedToken != null) {
            UserSession.token    = savedToken
            UserSession.username = prefs.getString("username", "User") ?: "User"
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupUI()
        observeViewModel()

        // הצגת הודעת הצלחה אם הגענו מאיפוס סיסמה
        if (intent.getBooleanExtra("password_reset_success", false)) {
            Snackbar.make(
                binding.root,
                "Password reset successfully! Please log in.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun setupUI() {
        binding.btnLogin?.setOnClickListener {
            val emailField    = binding.etEmail as? android.widget.EditText
            val passwordField = binding.etPassword as? android.widget.EditText

            val email    = emailField?.text?.toString()?.trim() ?: ""
            val password = passwordField?.text?.toString() ?: ""

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                showError("Please enter email and password")
            }
        }

        binding.btnRegister?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // כפתור "שכחתי סיסמה"
        binding.tvForgotPassword?.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> showLoading()
                is LoginState.Success -> handleLoginSuccess(state)
                is LoginState.Error   -> {
                    hideLoading()
                    showError(state.message)
                }
                else -> hideLoading()
            }
        }
    }

    private fun handleLoginSuccess(state: LoginState.Success) {
        UserSession.token    = state.response.token
        UserSession.username = state.response.user?.name ?: "User"

        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("token",    state.response.token)
            putString("username", UserSession.username)
            putString("user_id",  state.response.user?.id)
            apply()
        }

        Toast.makeText(this, "Welcome back, ${UserSession.username}!", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading() {
        binding.progressBar?.visibility = View.VISIBLE
        binding.btnLogin?.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar?.visibility = View.GONE
        binding.btnLogin?.isEnabled = true
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(
            this,
            message,
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}