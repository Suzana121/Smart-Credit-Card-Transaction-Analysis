package com.cardify.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.databinding.ActivityVerifyOtpBinding
import com.google.android.material.snackbar.Snackbar

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding
    private val viewModel: ResetPasswordViewModel by viewModels()
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra("email") ?: ""
        binding.tvEmailHint.text = "Code sent to: $email"

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnResetPassword.setOnClickListener {
            val otp             = binding.etOtp.text.toString().trim()
            val newPassword     = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            viewModel.resetPassword(email, otp, newPassword, confirmPassword)
        }

        binding.tvResendCode.setOnClickListener {
            // חזרה למסך הקודם כדי לשלוח קוד חדש
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

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnResetPassword.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnResetPassword.isEnabled = true
    }
}