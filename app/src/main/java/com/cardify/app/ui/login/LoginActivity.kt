package com.cardify.app.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cardify.app.MainActivity
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.data.repository.AuthRepository
import com.cardify.app.databinding.ActivityLoginBinding
import com.cardify.app.utils.PreferencesManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocationAndProceed()
        } else {
            startMainActivity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // בדיקה אם המשתמש כבר מחובר - עכשיו דרך PreferencesManager
        val prefs = PreferencesManager.getInstance(this)
        val savedToken = prefs.getToken()

        if (savedToken != null) {
            UserSession.token = savedToken
            UserSession.username = prefs.getUserName() ?: "User"
            UserSession.id = prefs.getUserId()
            UserSession.email = prefs.getUserEmail()
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnLogin?.setOnClickListener {
            Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show()
            val email = findViewById<android.widget.EditText>(R.id.etEmail)?.text?.toString()?.trim() ?: ""
            val password = findViewById<android.widget.EditText>(R.id.etPassword)?.text?.toString() ?: ""

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
        val token = state.response.token ?: return
        val name = state.response.user?.name ?: "User"
        val userId = state.response.user?.id ?: ""
        val email = state.response.user?.email ?: ""

        // שמירה ל-UserSession (in-memory)
        UserSession.token = token
        UserSession.username = name
        UserSession.id = userId
        UserSession.email = email
        UserSession.role = state.response.user?.role ?: "user"

        // שמירה ל-PreferencesManager (persistent) - מקום אחד בלבד!
        val prefs = PreferencesManager.getInstance(this)
        prefs.saveToken(token)
        prefs.saveUserData(userId, email, name)

        Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()

        requestLocationOrProceed()
    }

    private fun requestLocationOrProceed() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            fineGranted || coarseGranted -> fetchLocationAndProceed()
            else -> requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocationAndProceed() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("LocationDebug", "lastLocation: lat=${location.latitude}, lng=${location.longitude}")
                        lifecycleScope.launch {
                            val result = AuthRepository().updateLocation(location.latitude, location.longitude)
                            if (result.isSuccess) android.util.Log.d("LocationDebug", "updateLocation succeeded")
                            else android.util.Log.e("LocationDebug", "updateLocation failed: ${result.exceptionOrNull()?.message}")
                            startMainActivity()
                        }
                    } else {
                        android.util.Log.d("LocationDebug", "lastLocation is null, trying getCurrentLocation...")
                        fetchCurrentLocationAndProceed(fusedClient)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("LocationDebug", "lastLocation failed: ${e.message}")
                    fetchCurrentLocationAndProceed(fusedClient)
                }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationDebug", "SecurityException: ${e.message}")
            startMainActivity()
        }
    }

    private fun fetchCurrentLocationAndProceed(fusedClient: FusedLocationProviderClient) {
        try {
            val cts = CancellationTokenSource()
            android.os.Handler(mainLooper).postDelayed({ cts.cancel() }, 10_000L)

            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    lifecycleScope.launch {
                        if (location != null) {
                            android.util.Log.d("LocationDebug", "getCurrentLocation: lat=${location.latitude}, lng=${location.longitude}")
                            val result = AuthRepository().updateLocation(location.latitude, location.longitude)
                            if (result.isSuccess) android.util.Log.d("LocationDebug", "updateLocation succeeded")
                            else android.util.Log.e("LocationDebug", "updateLocation failed: ${result.exceptionOrNull()?.message}")
                        } else {
                            android.util.Log.d("LocationDebug", "getCurrentLocation returned null")
                        }
                        startMainActivity()
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("LocationDebug", "getCurrentLocation failed: ${e.message}")
                    startMainActivity()
                }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationDebug", "SecurityException: ${e.message}")
            startMainActivity()
        }
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
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}