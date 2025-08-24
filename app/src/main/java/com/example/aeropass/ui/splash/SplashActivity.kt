package com.example.aeropass.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// Removed SplashScreen import to use custom layout
import androidx.lifecycle.lifecycleScope
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.R
import com.example.aeropass.ui.auth.AuthActivity
import com.example.aeropass.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // Simple delay without animations - professional and clean
        lifecycleScope.launch {
            delay(1500)
            navigateToNextActivity()
        }
    }
    
    private fun navigateToNextActivity() {
        val app = application as AeroPassApplication
        val intent = if (app.securityManager.isMasterPasswordSet()) {
            Intent(this@SplashActivity, AuthActivity::class.java)
        } else {
            Intent(this@SplashActivity, AuthActivity::class.java).apply {
                putExtra("setup_mode", true)
            }
        }
        
        startActivity(intent)
        finish()
    }
    

}