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

/**
 * Login Activity
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

    /**
     * Setup UI components and listeners
     */
    private fun setupUI() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }

        // Register button click
        binding.btnRegister?.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot Password - Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle -> {
                    hideLoading()
                }

                is LoginState.Loading -> {
                    showLoading()
                }

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

    /**
     * Handle successful login
     */
    private fun handleLoginSuccess(state: LoginState.Success) {
        val response = state.response

        // Save token
        response.token?.let { token ->
            preferencesManager.saveToken(token)
        }

        // Save user data
        response.user?.let { user ->
            preferencesManager.saveUserData(
                userId = user.id,
                email = user.email,
                name = user.name
            )
        }

        // Show success message
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

        // Navigate to home
        navigateToHome()
    }

    /**
     * Navigate to home screen
     */
    private fun navigateToHome() {
        Toast.makeText(this, "Navigating to Home...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Show loading state
     */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.etEmail.isEnabled = false
        binding.etPassword.isEnabled = false
    }

    /**
     * Hide loading state
     */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.isEnabled = true
        binding.etEmail.isEnabled = true
        binding.etPassword.isEnabled = true
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.status_error))
            .setTextColor(getColor(R.color.text_white))
            .show()
    }
}