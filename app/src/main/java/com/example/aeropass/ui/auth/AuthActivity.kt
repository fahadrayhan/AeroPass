package com.example.aeropass.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.R
import com.example.aeropass.databinding.ActivityAuthBinding
import com.example.aeropass.ui.main.MainActivity
import com.example.aeropass.utils.BiometricAuthManager
import java.util.concurrent.Executor

class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application)
    }
    private lateinit var biometricManager: BiometricAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        biometricManager = BiometricAuthManager(this)
        
        val isSetupMode = intent.getBooleanExtra("setup_mode", false)
        
        setupUI(isSetupMode)
        observeViewModel()
        
        // Auto-trigger biometric authentication if enabled and not in setup mode
        if (!isSetupMode) {
            checkAndTriggerAutoBiometric()
        }
    }
    
    private fun setupUI(isSetupMode: Boolean) {
        if (isSetupMode) {
            binding.titleText.text = "Setup Master Password"
            binding.confirmPasswordLayout.visibility = View.VISIBLE
            binding.biometricButton.visibility = View.GONE
            binding.loginButton.text = "Setup"
        } else {
            binding.titleText.text = "Enter Master Password"
            binding.confirmPasswordLayout.visibility = View.GONE
            
            // Show biometric button only if both available and enabled
            val app = application as AeroPassApplication
            val isBiometricEnabled = app.sessionManager.isBiometricEnabled()
            val isBiometricAvailable = biometricManager.isBiometricAvailable()
            
            binding.biometricButton.visibility = if (isBiometricEnabled && isBiometricAvailable) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            binding.loginButton.text = "Unlock"
        }
        
        binding.loginButton.setOnClickListener {
            val password = binding.passwordEditText.text.toString()
            if (isSetupMode) {
                val confirmPassword = binding.confirmPasswordEditText.text.toString()
                viewModel.setupMasterPassword(password, confirmPassword)
            } else {
                viewModel.authenticateWithPassword(password)
            }
        }
        
        binding.biometricButton.setOnClickListener {
            authenticateWithBiometric()
        }
    }

    private fun authenticateWithBiometric() {
        biometricManager.authenticateWithBiometric(
            activity = this,
            onSuccess = {
                viewModel.authenticateWithBiometric()
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            },
            onFailed = {
                Toast.makeText(this, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun checkAndTriggerAutoBiometric() {
        val app = application as AeroPassApplication
        val isBiometricEnabled = app.sessionManager.isBiometricEnabled()
        val isBiometricAvailable = biometricManager.isBiometricAvailable()
        
        // Auto-trigger biometric authentication if both enabled and available
        if (isBiometricEnabled && isBiometricAvailable) {
            // Add a small delay to ensure UI is fully loaded
            binding.root.postDelayed({
                authenticateWithBiometric()
            }, 300)
        }
    }
    
    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Authenticated -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }
}