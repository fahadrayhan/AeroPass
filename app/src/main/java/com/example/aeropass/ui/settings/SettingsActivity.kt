package com.example.aeropass.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aeropass.AboutActivity
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.R
import com.example.aeropass.databinding.ActivitySettingsBinding
import com.example.aeropass.utils.BiometricAuthManager
import com.example.aeropass.utils.SessionManager
import com.example.aeropass.ui.auth.AuthActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(
            (application as AeroPassApplication).securityManager,
            (application as AeroPassApplication).sessionManager,
            (application as AeroPassApplication).repository,
            this
        )
    }
    private lateinit var biometricAuthManager: BiometricAuthManager
    private var exportType: SettingsViewModel.ExportType = SettingsViewModel.ExportType.ENCRYPTED
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupBiometricAuth()
        setupTheme()
        setupAutoLock()
        setupGoogleDriveBackup()
        setupClickListeners()
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        val app = application as AeroPassApplication
        app.sessionManager.resetAutoLockTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        val app = application as AeroPassApplication
        app.sessionManager.resetAutoLockTimer()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupBiometricAuth() {
        biometricAuthManager = BiometricAuthManager(this)
        
        // Check if biometric authentication is available
        val isBiometricAvailable = biometricAuthManager.isBiometricAvailable()
        binding.switchBiometric.isEnabled = isBiometricAvailable
        
        if (!isBiometricAvailable) {
            binding.switchBiometric.isChecked = false
            // You might want to show a message explaining why biometric auth is not available
        } else {
            binding.switchBiometric.isChecked = viewModel.isBiometricEnabled()
        }
        
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && isBiometricAvailable) {
                // Enable biometric authentication
                viewModel.setBiometricEnabled(true)
            } else {
                // Disable biometric authentication
                viewModel.setBiometricEnabled(false)
            }
        }
    }
    
    private fun setupAutoLock() {
        binding.switchAutoLock.isChecked = viewModel.getAutoLockTimeout() > 0
        binding.layoutLockAfter.visibility = if (binding.switchAutoLock.isChecked) View.VISIBLE else View.GONE

        binding.switchAutoLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setAutoLockTimeout(5 * 60 * 1000) // Default to 5 minutes
                binding.layoutLockAfter.visibility = View.VISIBLE
            } else {
                viewModel.setAutoLockTimeout(-1L) // Never
                binding.layoutLockAfter.visibility = View.GONE
            }
            updateAutoLockSummary()
        }

        binding.layoutLockAfter.setOnClickListener {
            showAutoLockDialog()
        }
        updateAutoLockSummary()
    }

    private fun showAutoLockDialog() {
        val timeoutOptions = arrayOf(
            "30 seconds" to 30L * 1000,
            "1 minute" to 60L * 1000,
            "2 minutes" to 2L * 60 * 1000,
            "5 minutes" to 5L * 60 * 1000,
            "10 minutes" to 10L * 60 * 1000,
            "15 minutes" to 15L * 60 * 1000,
            "30 minutes" to 30L * 60 * 1000,
            "1 hour" to 60L * 60 * 1000
        )

        val currentTimeout = viewModel.getAutoLockTimeout()
        val currentIndex = timeoutOptions.indexOfFirst { it.second == currentTimeout }

        AlertDialog.Builder(this)
            .setTitle("Lock after")
            .setSingleChoiceItems(timeoutOptions.map { it.first }.toTypedArray(), currentIndex) { dialog, which ->
                val selectedTimeout = timeoutOptions[which].second
                viewModel.setAutoLockTimeout(selectedTimeout)
                updateAutoLockSummary()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAutoLockSummary() {
        val timeout = viewModel.getAutoLockTimeout()
        val summary = when {
            timeout < 0 -> "Never"
            timeout < 60000 -> "${timeout / 1000} seconds"
            else -> "${timeout / 60000} minutes"
        }
        binding.tvAutoLockSummary.text = summary
        binding.tvAutoLockStatus.text = if (timeout > 0) "ON" else "OFF"
    }
    
    private fun setupGoogleDriveBackup() {
        // Initialize Google Drive backup switch state
        binding.switchGoogleDriveBackup.isChecked = viewModel.isGoogleDriveBackupEnabled()
        
        // Update status text based on current state
        updateGoogleDriveStatus()
        
        binding.switchGoogleDriveBackup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Enable Google Drive backup
                enableGoogleDriveBackup()
            } else {
                // Disable Google Drive backup
                viewModel.setGoogleDriveBackupEnabled(false)
                updateGoogleDriveStatus()
            }
        }
        
        // Manual backup button
        binding.layoutGoogleDriveBackup.setOnClickListener {
            if (viewModel.isGoogleDriveBackupEnabled()) {
                performManualBackup()
            } else {
                Toast.makeText(this, "Please enable Google Drive backup first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun enableGoogleDriveBackup() {
        Toast.makeText(this, "This Feature implement in next updates. Thanks", Toast.LENGTH_LONG).show()
        binding.switchGoogleDriveBackup.isChecked = false
    }
    
    private fun performManualBackup() {
        AlertDialog.Builder(this)
            .setTitle("Manual Backup")
            .setMessage("This will backup your data to Google Drive now.")
            .setPositiveButton("Backup") { _, _ ->
                lifecycleScope.launch {
                    val success = viewModel.performGoogleDriveBackup()
                    if (success) {
                        Toast.makeText(this@SettingsActivity, "Backup completed successfully", Toast.LENGTH_SHORT).show()
                        updateGoogleDriveStatus()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Backup failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateGoogleDriveStatus() {
        if (viewModel.isGoogleDriveBackupEnabled()) {
            val lastBackup = viewModel.getLastBackupTime()
            binding.tvGoogleDriveStatus.text = if (lastBackup.isNotEmpty()) {
                "Last backup: $lastBackup"
            } else {
                "Backup enabled - No backups yet"
            }
        } else {
            binding.tvGoogleDriveStatus.text = "Backup disabled"
        }
    }
    
    private fun setupClickListeners() {
        binding.layoutChangeMasterPassword.setOnClickListener {
            showChangeMasterPasswordDialog()
        }

        binding.layoutLogout.setOnClickListener {
            viewModel.logout()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        binding.layoutExportData.setOnClickListener {
            exportData()
        }
        
        binding.layoutImportData.setOnClickListener {
            importData()
        }
        
        binding.layoutAboutApp.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is SettingsUiState.Loading -> {
                    // Show loading indicator if needed
                }
                is SettingsUiState.Success -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Observe Google Drive backup status changes
        viewModel.googleDriveBackupStatus.observe(this) { status ->
            updateGoogleDriveStatus()
        }
    }
    
    private fun showChangeMasterPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_master_password, null)
        val currentPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.til_current_password)
        val currentPasswordEdit = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val newPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.til_new_password)
        val newPasswordEdit = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.til_confirm_password)
        val confirmPasswordEdit = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)
        
        AlertDialog.Builder(this)
            .setTitle("Change Master Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordEdit.text.toString()
                val newPassword = newPasswordEdit.text.toString()
                val confirmPassword = confirmPasswordEdit.text.toString()
                
                // Reset errors
                currentPasswordLayout.error = null
                newPasswordLayout.error = null
                confirmPasswordLayout.error = null
                
                // Validate inputs
                var hasError = false
                
                if (currentPassword.isEmpty()) {
                    currentPasswordLayout.error = "Current password is required"
                    hasError = true
                }
                
                if (newPassword.isEmpty()) {
                    newPasswordLayout.error = "New password is required"
                    hasError = true
                } else if (newPassword.length < 6) {
                    newPasswordLayout.error = "Password must be at least 6 characters"
                    hasError = true
                }
                
                if (confirmPassword != newPassword) {
                    confirmPasswordLayout.error = "Passwords do not match"
                    hasError = true
                }
                
                if (!hasError) {
                    lifecycleScope.launch {
                        viewModel.changeMasterPassword(currentPassword, newPassword)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportData() {
        val options = arrayOf("Encrypted JSON (.aes)", "Plain JSON (.json)")
        var selectedExportType = SettingsViewModel.ExportType.ENCRYPTED

        AlertDialog.Builder(this)
            .setTitle("Export Format")
            .setSingleChoiceItems(options, 0) { _, which ->
                selectedExportType = if (which == 0) {
                    SettingsViewModel.ExportType.ENCRYPTED
                } else {
                    SettingsViewModel.ExportType.PLAIN_JSON
                }
            }
            .setPositiveButton("Export") { _, _ ->
                exportDataWithFormat(selectedExportType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportDataWithFormat(exportType: SettingsViewModel.ExportType) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = if (exportType == SettingsViewModel.ExportType.ENCRYPTED) ".aes" else ".json"
            putExtra(Intent.EXTRA_TITLE, "aeropass_backup_$timestamp$extension")
        }
        // Store the export type for use in onActivityResult
        this.exportType = exportType
        startActivityForResult(intent, REQUEST_CODE_EXPORT)
    }
    
    private fun importData() {
        AlertDialog.Builder(this)
            .setTitle("Import Data")
            .setMessage("This will import credentials from a JSON file (encrypted or regular). Supports all credential types: Login, Payment, Identity, and Secure Notes. Existing credentials with the same title will be overwritten.")
            .setPositiveButton("Import") { _, _ ->
                // Launch file picker
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*" // Accept all file types to support both .json and .aes files
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(intent, REQUEST_CODE_IMPORT)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    viewModel.importData(uri)
                }
            }
        } else if (requestCode == REQUEST_CODE_EXPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    viewModel.exportData(uri, this@SettingsActivity.exportType)
                }
            }
        }
    }
    


    companion object {
        private const val REQUEST_CODE_IMPORT = 1001
        private const val REQUEST_CODE_EXPORT = 1002
    }
    
    private fun setupTheme() {
        val currentTheme = viewModel.getThemeMode()
        when (currentTheme) {
            SessionManager.THEME_MODE_LIGHT -> binding.rgTheme.check(R.id.rb_light)
            SessionManager.THEME_MODE_DARK -> binding.rgTheme.check(R.id.rb_dark)
            else -> binding.rgTheme.check(R.id.rb_follow_system)
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedTheme = when (checkedId) {
                R.id.rb_light -> SessionManager.THEME_MODE_LIGHT
                R.id.rb_dark -> SessionManager.THEME_MODE_DARK
                else -> SessionManager.THEME_MODE_SYSTEM
            }
            viewModel.setThemeMode(selectedTheme)
            (application as AeroPassApplication).setTheme(selectedTheme)
            updateThemeSummary()
        }
        updateThemeSummary()
    }

    private fun updateThemeSummary() {
        val themeName = when (viewModel.getThemeMode()) {
            SessionManager.THEME_MODE_LIGHT -> "Light"
            SessionManager.THEME_MODE_DARK -> "Dark"
            else -> "Follow System"
        }
        binding.tvThemeSummary.text = "Light / Dark / System default"
    }
}