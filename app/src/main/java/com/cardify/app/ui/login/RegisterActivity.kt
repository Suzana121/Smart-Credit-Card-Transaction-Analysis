package com.cardify.app.ui.login


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Register Activity
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    /**
     * Setup UI components and listeners
     */
    private fun setupUI() {
        // Register button click
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInputs(username, email, phone, password, confirmPassword)) {
                performRegistration(username, email, phone, password)
            }
        }

        // Already have account - go to login
        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish() // Go back to login screen
        }
    }

    /**
     * Validate user inputs
     */
    private fun validateInputs(
        username: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        // Validate username
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            return false
        }
        binding.tilUsername.error = null

        // Validate email
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            return false
        }
        binding.tilEmail.error = null

        // Validate phone
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            return false
        }
        binding.tilPhone.error = null

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }
        binding.tilPassword.error = null

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return false
        }
        binding.tilConfirmPassword.error = null

        return true
    }

    /**
     * Perform registration
     */
    private fun performRegistration(
        username: String,
        email: String,
        phone: String,
        password: String
    ) {
        showLoading()

        // TODO: Implement actual registration API call
        // For now, just show success message

        // Simulate network delay
        binding.root.postDelayed({
            hideLoading()

            Toast.makeText(
                this,
                "Registration successful! Please login.",
                Toast.LENGTH_LONG
            ).show()

            // Go back to login screen
            finish()
        }, 1500)
    }

    /**
     * Show loading state
     */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        binding.etUsername.isEnabled = false
        binding.etEmail.isEnabled = false
        binding.etPhone.isEnabled = false
        binding.etPassword.isEnabled = false
        binding.etConfirmPassword.isEnabled = false
    }

    /**
     * Hide loading state
     */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
        binding.etUsername.isEnabled = true
        binding.etEmail.isEnabled = true
        binding.etPhone.isEnabled = true
        binding.etPassword.isEnabled = true
        binding.etConfirmPassword.isEnabled = true
    }
}
