package com.example.aeropass


import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aeropass.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupVersionInfo()
        setupSocialButtons()

        setupSystemInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = "Version $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "Version 1.0.0"
        }
    }

    private fun setupSocialButtons() {
        // GitHub button
        binding.btnGithub.setOnClickListener {
            openUrl("https://github.com/fahadrayhan")
        }

        // Email button
        binding.btnEmail.setOnClickListener {
            sendEmail()
        }

        // LinkedIn button
        binding.btnLinkedin.setOnClickListener {
            openUrl("https://linkedin.com/in/fahadrayhan")
        }

        // Facebook button
        binding.btnFacebook.setOnClickListener {
            openUrl("https://facebook.com/fahad.rayhan")
        }
    }



    private fun setupSystemInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            
            // Build version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                binding.tvBuildVersion.text = packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                binding.tvBuildVersion.text = packageInfo.versionCode.toString()
            }
            
            // Target SDK
            binding.tvTargetSdk.text = applicationInfo.targetSdkVersion.toString()
            
            // Min SDK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.tvMinSdk.text = applicationInfo.minSdkVersion.toString()
            } else {
                binding.tvMinSdk.text = "24" // Default min SDK
            }
            
        } catch (e: PackageManager.NameNotFoundException) {
            // Set default values if package info is not available
            binding.tvBuildVersion.text = "1"
            binding.tvTargetSdk.text = "34"
            binding.tvMinSdk.text = "24"
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:fahadrayhan123@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "AeroPass - Feedback")
                putExtra(Intent.EXTRA_TEXT, "Hello Fahad,\n\nI have some feedback about AeroPass:\n\n")
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}