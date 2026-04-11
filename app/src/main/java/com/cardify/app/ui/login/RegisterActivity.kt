package com.cardify.app.ui.login

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cardify.app.databinding.ActivityRegisterBinding

/**
 * Activity that hosts the new-account registration form.
 *
 * Collects username, email, phone, password, and confirm-password from the user,
 * then delegates to [RegisterViewModel] to submit the request. Navigates back to
 * [LoginActivity] on success.
 */
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

    /**
     * Wires up button click listeners and navigates back to the login screen when the
     * "already have an account" link is tapped.
     */
    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username = (binding.etUsername as? EditText)?.text.toString().trim()
            val email = (binding.etEmail as? EditText)?.text.toString().trim()
            val phone = (binding.etPhone as? EditText)?.text.toString().trim()
            val password = (binding.etPassword as? EditText)?.text.toString()
            val confirmPassword = (binding.etConfirmPassword as? EditText)?.text.toString()

            val errorMsg = when {
                username.isEmpty()  -> "Username cannot be empty."
                username.length < 3 -> "Username must be at least 3 characters."
                email.isEmpty()     -> "Email cannot be empty."
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    "Please enter a valid email address."
                phone.isEmpty()     -> "Phone number cannot be empty."
                !phone.all { it.isDigit() } -> "Phone number must contain digits only."
                phone.length < 9 || phone.length > 10 ->
                    "Phone number must be 9–10 digits."
                password.isEmpty()  -> "Password cannot be empty."
                password.length < 6 -> "Password must be at least 6 characters."
                password != confirmPassword -> "Passwords do not match."
                else -> null
            }

            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            } else {
                viewModel.register(username, email, phone, password)
            }
        }

        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish()
        }
    }

    /**
     * Observes [RegisterViewModel.registerState] and updates the UI accordingly:
     * shows a loading indicator while the request is in flight, finishes the activity
     * on success, or shows an error toast on failure.
     */
    private fun observeViewModel() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterState.Loading -> showLoading()
                is RegisterState.Success -> {
                    hideLoading()
                    com.cardify.app.utils.PreferencesManager.getInstance(this).clearAll()
                    Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is RegisterState.Error -> {
                    hideLoading()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> hideLoading()
            }
        }
    }

    /** Shows the progress bar and disables the register button. */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
    }

    /** Hides the progress bar and re-enables the register button. */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
    }
}
