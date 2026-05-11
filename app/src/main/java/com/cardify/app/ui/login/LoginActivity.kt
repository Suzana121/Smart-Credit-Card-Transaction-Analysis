package com.cardify.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cardify.app.MainActivity
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(applicationContext)
        // בדיקה אם המשתמש כבר מחובר
        val prefs      = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)
        val reason = intent.getStringExtra("logout_reason")
        if (reason == "session_expired") {
            // הודעה חמודה וברורה
            Toast.makeText(this, "Session timeout. Please log in again.", Toast.LENGTH_LONG).show()
        }
        if (savedToken != null) {
            UserSession.token    = savedToken
            UserSession.username = prefs.getString("username", "User") ?: "User"
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        etEmail    = binding.root.findViewById(R.id.etEmail)
        etPassword = binding.root.findViewById(R.id.etPassword)

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
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            clearFieldErrors()
            if (email.isEmpty() && password.isEmpty()) {
                setFieldError(etEmail, binding.tvEmailError, "Please Enter User Name")
                setFieldError(etPassword, binding.tvPasswordError, "Please enter password")
            }  else {
                viewModel.login(email, password)
            }
        }

        // Clear errors when user starts typing
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { clearFieldError(etEmail, binding.tvEmailError) }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { clearFieldError(etPassword, binding.tvPasswordError) }
        })

        // Password visibility toggle
        val ivPasswordToggle = binding.root.findViewById<ImageView>(R.id.ivPasswordToggle)
        ivPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_open)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivPasswordToggle.setImageResource(R.drawable.ic_eye_closed)
            }
            etPassword.setSelection(etPassword.text.length)
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
        val emailEmpty    = etEmail.text.isNullOrEmpty()
        val passwordEmpty = etPassword.text.isNullOrEmpty()

        clearFieldErrors()

        when {
            emailEmpty && passwordEmpty -> {
                setFieldError(etEmail, binding.tvEmailError, message)
                setFieldError(etPassword, binding.tvPasswordError, message)
            }
            emailEmpty    -> setFieldError(etEmail, binding.tvEmailError, message)
            passwordEmpty -> setFieldError(etPassword, binding.tvPasswordError, message)
            else          -> setFieldError(etEmail, binding.tvEmailError, message) // API error
        }
    }

    private fun setFieldError(field: EditText, errorView: TextView?, message: String) {
        field.setBackgroundResource(R.drawable.input_field_error)
        errorView?.text = message
        errorView?.visibility = View.VISIBLE
    }

    private fun clearFieldError(field: EditText, errorView: TextView?) {
        field.setBackgroundResource(R.drawable.input_field_rounded)
        errorView?.visibility = View.GONE
    }

    private fun clearFieldErrors() {
        clearFieldError(etEmail, binding.tvEmailError)
        clearFieldError(etPassword, binding.tvPasswordError)
    }
}
