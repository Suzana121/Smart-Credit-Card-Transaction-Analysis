package com.cardify.app.ui.login

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cardify.app.databinding.ActivityForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var viewModel: ForgotPasswordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ForgotPasswordViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSendCode.setOnClickListener {
            viewModel.sendResetLink(binding.etEmail.text.toString())
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ForgotPasswordState.Loading -> showLoading()

                is ForgotPasswordState.LinkSent -> {
                    hideLoading()
                    binding.formContainer.visibility = View.GONE
                    binding.successContainer.visibility = View.VISIBLE
                }

                is ForgotPasswordState.Error -> {
                    hideLoading()
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }

                is ForgotPasswordState.Idle -> hideLoading()
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendCode.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnSendCode.isEnabled = true
    }
}
