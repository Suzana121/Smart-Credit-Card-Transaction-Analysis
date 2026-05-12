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

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(applicationContext)
        val prefs      = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)
        val reason = intent.getStringExtra("logout_reason")
        if (reason == "session_expired") {
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

        etUsername = binding.root.findViewById(R.id.etUsername)
        etPassword = binding.root.findViewById(R.id.etPassword)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupUI()
        observeViewModel()

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
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            clearFieldStyles()
            viewModel.login(username, password)
        }

        etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { resetFieldStyle(etUsername, binding.tvUsernameError) }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { resetFieldStyle(etPassword, binding.tvPasswordError) }
        })

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

        binding.tvForgotPassword?.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> showLoading()
                is LoginState.Success -> handleLoginSuccess(state)

                is LoginState.BothEmpty -> {
                    hideLoading()
                    setFieldError(etUsername, binding.tvUsernameError, "Please Enter User Name")
                    setFieldError(etPassword, binding.tvPasswordError, "Please Enter Password")
                }
                is LoginState.UsernameEmpty -> {
                    hideLoading()
                    setFieldError(etUsername, binding.tvUsernameError, "Please Enter User Name")
                    // לא מסמנים ירוק על סיסמא - לא אומתה עדיין
                }
                is LoginState.PasswordEmpty -> {
                    hideLoading()
                    setFieldError(etPassword, binding.tvPasswordError, "Please Enter Password")
                    // לא מסמנים ירוק על שם - לא אומת עדיין
                }
                is LoginState.PasswordTooShort -> {
                    hideLoading()
                    setFieldError(etPassword, binding.tvPasswordError, "Password Must Be At Least 6 Characters")
                    // לא מסמנים ירוק על שם - לא אומת עדיין
                }
                is LoginState.UserNotFound -> {
                    hideLoading()
                    setFieldError(etUsername, binding.tvUsernameError, "No Account Found With This User Name")
                    // לא מסמנים כלום על סיסמא - לא יודעים אם היא נכונה
                }
                is LoginState.WrongPassword -> {
                    hideLoading()
                    // רק כאן אנחנו יודעים בוודאות שהשם נכון
                    setFieldSuccess(etUsername, binding.tvUsernameError)
                    setFieldError(etPassword, binding.tvPasswordError, "Incorrect Password")
                }
                is LoginState.Error -> {
                    hideLoading()
                    setFieldError(etPassword, binding.tvPasswordError, state.message)
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

    private fun setFieldSuccess(field: EditText, errorView: TextView?) {
        field.setBackgroundResource(R.drawable.input_field_success)
        errorView?.visibility = View.GONE
    }

    private fun setFieldError(field: EditText, errorView: TextView?, message: String) {
        field.setBackgroundResource(R.drawable.input_field_error)
        errorView?.text = message
        errorView?.visibility = View.VISIBLE
    }

    private fun resetFieldStyle(field: EditText, errorView: TextView?) {
        field.setBackgroundResource(R.drawable.input_field_rounded)
        errorView?.visibility = View.GONE
    }

    private fun clearFieldStyles() {
        resetFieldStyle(etUsername, binding.tvUsernameError)
        resetFieldStyle(etPassword, binding.tvPasswordError)
    }
}