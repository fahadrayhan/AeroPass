package com.example.aeropass.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*

class SessionManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_CLIPBOARD_TIMEOUT = "clipboard_timeout"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_GOOGLE_DRIVE_BACKUP_ENABLED = "google_drive_backup_enabled"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val DEFAULT_AUTO_LOCK_TIMEOUT = 5 * 60 * 1000L // 5 minutes
        private const val DEFAULT_CLIPBOARD_TIMEOUT = 15 * 1000L // 15 seconds
        
        // Theme mode constants
        const val THEME_MODE_SYSTEM = 0
        const val THEME_MODE_LIGHT = 1
        const val THEME_MODE_DARK = 2
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isAuthenticated = MutableLiveData<Boolean>(false)
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    private var autoLockJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    var autoLockTimeout: Long
        get() = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_AUTO_LOCK_TIMEOUT)
        set(value) = prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, value).apply()
    
    var clipboardTimeout: Long
        get() = prefs.getLong(KEY_CLIPBOARD_TIMEOUT, DEFAULT_CLIPBOARD_TIMEOUT)
        set(value) = prefs.edit().putLong(KEY_CLIPBOARD_TIMEOUT, value).apply()
    
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()
    
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
    
    fun authenticate() {
        _isAuthenticated.value = true
        startAutoLockTimer()
    }
    
    fun logout() {
        _isAuthenticated.value = false
        stopAutoLockTimer()
    }
    
    fun resetAutoLockTimer() {
        if (_isAuthenticated.value == true) {
            startAutoLockTimer()
        }
    }
    
    private fun startAutoLockTimer() {
        stopAutoLockTimer()
        autoLockJob = scope.launch {
            delay(autoLockTimeout)
            logout()
        }
    }
    
    private fun stopAutoLockTimer() {
        autoLockJob?.cancel()
        autoLockJob = null
    }
    
    fun cleanup() {
        scope.cancel()
    }
    
    // Google Drive backup methods
    fun isGoogleDriveBackupEnabled(): Boolean {
        return prefs.getBoolean(KEY_GOOGLE_DRIVE_BACKUP_ENABLED, false)
    }
    
    fun setGoogleDriveBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GOOGLE_DRIVE_BACKUP_ENABLED, enabled).apply()
    }
    
    fun getLastBackupTime(): Long {
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }
    
    fun setLastBackupTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, time).apply()
    }
}