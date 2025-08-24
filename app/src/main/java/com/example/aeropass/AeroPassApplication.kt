package com.example.aeropass

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.aeropass.data.database.AppDatabase
import com.example.aeropass.data.repository.CredentialRepository
import com.example.aeropass.utils.SecurityManager
import com.example.aeropass.utils.SessionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AeroPassApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CredentialRepository(database.credentialDao(), securityManager) }
    val securityManager by lazy { SecurityManager(this) }
    val sessionManager by lazy { SessionManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        applyTheme()
    }
    
    private fun applyTheme() {
        val nightMode = when (sessionManager.themeMode) {
            SessionManager.THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SessionManager.THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
    
    override fun setTheme(themeMode: Int) {
        sessionManager.themeMode = themeMode
        applyTheme()
    }
    
    companion object {
        lateinit var instance: AeroPassApplication
            private set
    }
}