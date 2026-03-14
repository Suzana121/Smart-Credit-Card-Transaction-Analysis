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

        // 1. בדיקה אם המשתמש כבר מחובר (לפני ה-setContentView)
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)

        if (savedToken != null) {
            // שחזור הנתונים ל-UserSession לשימוש בשאר האפליקציה
            UserSession.token = savedToken
            UserSession.username = prefs.getString("username", "User") ?: "User"

            // מעבר מהיר למסך הבית
            startMainActivity()
            return // עוצר את המשך ה-onCreate
        }

        // 2. אם לא מחובר, ממשיכים כרגיל בטעינת המסך
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnLogin?.setOnClickListener {
            val emailField = binding.etEmail as? android.widget.EditText
            val passwordField = binding.etPassword as? android.widget.EditText

            val email = emailField?.text?.toString()?.trim() ?: ""
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
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> showLoading()
                is LoginState.Success -> handleLoginSuccess(state)
                is LoginState.Error -> {
                    hideLoading()
                    showError(state.message)
                }
                else -> hideLoading()
            }
        }
    }

    private fun handleLoginSuccess(state: LoginState.Success) {
        // שמירה לזיכרון המיידי (UserSession)
        UserSession.token = state.response.token
        UserSession.username = state.response.user?.name ?: "User"

        // שמירה לדיסק (Persistent Storage)
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("token", state.response.token)
            putString("username", UserSession.username)
            putString("user_id", state.response.user?.id)
            apply()
        }

        Toast.makeText(this, "Welcome back, ${UserSession.username}!", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }

    // פונקציית עזר למעבר למסך הראשי וניקוי המחסנית
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
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}