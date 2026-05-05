package com.cardify.app.ui.login

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username = (binding.etUsername as? EditText)?.text.toString().trim()
            val email = (binding.etEmail as? EditText)?.text.toString().trim()
            val phone = (binding.etPhone as? EditText)?.text.toString().trim()
            val password = (binding.etPassword as? EditText)?.text.toString()
            val confirmPassword = (binding.etConfirmPassword as? EditText)?.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password == confirmPassword) {
                viewModel.register(username, email, phone, password ?: "")
            } else {
                Toast.makeText(this, "Please check your details", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish() // חוזר ללוגין
        }
    }

    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterState.Loading -> showLoading()
                is RegisterState.Success -> {
                    hideLoading()
                    // ניקוי נתונים ישנים כדי למנוע ערבוב עסקאות
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
    private fun showError(message: String) {
        android.widget.Toast.makeText(
            this,
            message,
            android.widget.Toast.LENGTH_LONG
        ).show()
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