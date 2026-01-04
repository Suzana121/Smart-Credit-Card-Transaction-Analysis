package com.cardify.app.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.databinding.ActivityRegisterBinding

/**
 * Register Activity - מעודכן עם חיבור ל-ViewModel
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    // חיבור ל-ViewModel (דורש implementation "androidx.activity:activity-ktx" ב-build.gradle)
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // אתחול ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    /**
     * הגדרת כפתורים ומאזינים
     */
    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInputs(username, email, phone, password, confirmPassword)) {
                // שליחה ל-ViewModel לביצוע הרישום האמיתי בשרת
                viewModel.register(username, email, phone, password)
            }
        }

        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish() // חזרה למסך הלוגין
        }
    }

    /**
     * האזנה לשינויים במצב הרישום מה-ViewModel
     */
    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterState.Loading -> showLoading()
                is RegisterState.Success -> {
                    hideLoading()
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                    finish() // סגירת המסך וחזרה ללוגין
                }
                is RegisterState.Error -> {
                    hideLoading()
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is RegisterState.Idle -> hideLoading()
            }
        }
    }

    /**
     * בדיקת תקינות קלטים
     */
    private fun validateInputs(
        username: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        } else binding.tilUsername.error = null

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email is required"
            isValid = false
        } else binding.tilEmail.error = null

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            isValid = false
        } else binding.tilPhone.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else binding.tilConfirmPassword.error = null

        return isValid
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