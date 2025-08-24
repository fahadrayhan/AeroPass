package com.example.aeropass.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.utils.BiometricAuthManager
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as AeroPassApplication
    private val securityManager = app.securityManager
    private val sessionManager = app.sessionManager
    private val biometricManager = BiometricAuthManager(application)
    
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    private val _isBiometricAvailable = MutableLiveData<Boolean>()
    val isBiometricAvailable: LiveData<Boolean> = _isBiometricAvailable
    
    init {
        checkBiometricAvailability()
        checkMasterPasswordStatus()
    }
    
    private fun checkBiometricAvailability() {
        _isBiometricAvailable.value = biometricManager.isBiometricAvailable()
    }
    
    private fun checkMasterPasswordStatus() {
        if (securityManager.isMasterPasswordSet()) {
            _authState.value = AuthState.RequireAuthentication
        } else {
            _authState.value = AuthState.RequireSetup
        }
    }
    
    fun setupMasterPassword(password: String, confirmPassword: String) {
        viewModelScope.launch {
            try {
                if (password != confirmPassword) {
                    _authState.value = AuthState.Error("Passwords do not match")
                    return@launch
                }
                
                if (password.length < 8) {
                    _authState.value = AuthState.Error("Password must be at least 8 characters")
                    return@launch
                }
                
                securityManager.saveMasterPassword(password)
                sessionManager.authenticate()
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to setup master password: ${e.message}")
            }
        }
    }
    
    fun authenticateWithPassword(password: String) {
        viewModelScope.launch {
            try {
                if (securityManager.verifyMasterPassword(password)) {
                    sessionManager.authenticate()
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Invalid password")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Authentication failed: ${e.message}")
            }
        }
    }
    
    fun authenticateWithBiometric() {
        sessionManager.authenticate()
        _authState.value = AuthState.Authenticated
    }
    
    fun logout() {
        sessionManager.logout()
        _authState.value = AuthState.RequireAuthentication
    }
    
    sealed class AuthState {
        object RequireSetup : AuthState()
        object RequireAuthentication : AuthState()
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }
}