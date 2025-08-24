package com.example.aeropass.ui.settings

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aeropass.data.repository.CredentialRepository
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.utils.SecurityManager
import com.example.aeropass.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(
    private val securityManager: SecurityManager,
    private val sessionManager: SessionManager,
    private val repository: CredentialRepository,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableLiveData<SettingsUiState>()
    val uiState: LiveData<SettingsUiState> = _uiState
    
    private val _exportUri = MutableLiveData<Uri?>()
    val exportUri: LiveData<Uri?> = _exportUri
    
    private val _googleDriveBackupStatus = MutableLiveData<Boolean>()
    val googleDriveBackupStatus: LiveData<Boolean> = _googleDriveBackupStatus
    
    fun isBiometricEnabled(): Boolean {
        return sessionManager.isBiometricEnabled()
    }
    
    fun setBiometricEnabled(enabled: Boolean) {
        sessionManager.setBiometricEnabled(enabled)
    }
    
    fun getAutoLockTimeout(): Long {
        return sessionManager.autoLockTimeout
    }
    
    fun setAutoLockTimeout(timeout: Long) {
        sessionManager.autoLockTimeout = timeout
    }
    
    // Google Drive backup methods
    fun isGoogleDriveBackupEnabled(): Boolean {
        return sessionManager.isGoogleDriveBackupEnabled()
    }
    
    fun setGoogleDriveBackupEnabled(enabled: Boolean) {
        sessionManager.setGoogleDriveBackupEnabled(enabled)
        _googleDriveBackupStatus.value = enabled
    }
    
    suspend fun enableGoogleDriveBackup(): Boolean {
        return try {
            // TODO: Implement actual Google Drive integration
            setGoogleDriveBackupEnabled(true)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun performGoogleDriveBackup(): Boolean {
        return try {
            // TODO: Implement actual Google Drive backup
            sessionManager.setLastBackupTime(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getLastBackupTime(): String {
        val lastBackup = sessionManager.getLastBackupTime()
        return if (lastBackup > 0) {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastBackup))
        } else {
            ""
        }
    }
    
    suspend fun changeMasterPassword(currentPassword: String, newPassword: String) {
        try {
            _uiState.value = SettingsUiState.Loading
            
            if (!securityManager.verifyMasterPassword(currentPassword)) {
                _uiState.value = SettingsUiState.Error("Current password is incorrect")
                return
            }
            
            securityManager.changeMasterPassword(currentPassword, newPassword)
            _uiState.value = SettingsUiState.Success("Master password changed successfully")
            
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Failed to change master password: ${e.message}")
        }
    }
    
    suspend fun exportData(uri: Uri, exportType: ExportType = ExportType.ENCRYPTED) {
        try {
            _uiState.value = SettingsUiState.Loading
            
            val credentials = repository.getAllCredentials().first() // Collect the Flow to get List
            val exportData = JSONObject()
            exportData.put("version", "1.0")
            exportData.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()))
            exportData.put("exportType", exportType.name)
            
            val credentialsArray = JSONArray()
            credentials.forEach { credential ->
                val credentialObj = JSONObject()
                credentialObj.put("id", credential.id)
                credentialObj.put("title", credential.title)
                credentialObj.put("credentialType", credential.credentialType.name)
                
                when (exportType) {
                    ExportType.ENCRYPTED -> {
                        // Keep data encrypted for secure export
                        credentialObj.put("encryptedData", credential.encryptedData)
                    }
                    ExportType.PLAIN_JSON -> {
                        // Decrypt data for plain JSON export
                        try {
                            val decryptedData = securityManager.decryptData(credential.encryptedData)
                            val decryptedJson = JSONObject(decryptedData)
                            credentialObj.put("decryptedData", decryptedJson)
                        } catch (e: Exception) {
                            // If decryption fails, include empty object
                            credentialObj.put("decryptedData", JSONObject())
                        }
                    }
                }
                
                credentialObj.put("category", credential.category)
                credentialObj.put("tags", credential.tags)
                credentialObj.put("dateCreated", credential.dateCreated)
                credentialObj.put("lastUpdated", credential.lastUpdated)
                credentialObj.put("isFavorite", credential.isFavorite)
                credentialsArray.put(credentialObj)
            }
            exportData.put("credentials", credentialsArray)

            val finalData = when (exportType) {
                ExportType.ENCRYPTED -> {
                    // Encrypt the entire JSON string for secure export
                    securityManager.encryptData(exportData.toString())
                }
                ExportType.PLAIN_JSON -> {
                    // Keep as plain JSON
                    exportData.toString(2) // Pretty print with 2-space indentation
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(finalData.toByteArray(Charsets.UTF_8))
            }
            
            _exportUri.value = uri
            val exportTypeText = if (exportType == ExportType.ENCRYPTED) "encrypted" else "plain JSON"
            _uiState.value = SettingsUiState.Success("Data exported successfully as $exportTypeText")
            
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Failed to export data: ${e.message}")
        }
    }
    
    suspend fun exportDataToFile(file: File, exportType: ExportType = ExportType.ENCRYPTED) {
        try {
            _uiState.value = SettingsUiState.Loading
            
            val credentials = repository.getAllCredentials().first() // Collect the Flow to get List
            val exportData = JSONObject()
            exportData.put("version", "1.0")
            exportData.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()))
            exportData.put("exportType", exportType.name)
            
            val credentialsArray = JSONArray()
            credentials.forEach { credential ->
                val credentialObj = JSONObject()
                credentialObj.put("id", credential.id)
                credentialObj.put("title", credential.title)
                credentialObj.put("credentialType", credential.credentialType.name)
                
                when (exportType) {
                    ExportType.ENCRYPTED -> {
                        // Keep data encrypted for secure export
                        credentialObj.put("encryptedData", credential.encryptedData)
                    }
                    ExportType.PLAIN_JSON -> {
                        // Decrypt data for plain JSON export
                        try {
                            val decryptedData = securityManager.decryptData(credential.encryptedData)
                            val decryptedJson = JSONObject(decryptedData)
                            credentialObj.put("decryptedData", decryptedJson)
                        } catch (e: Exception) {
                            // If decryption fails, include empty object
                            credentialObj.put("decryptedData", JSONObject())
                        }
                    }
                }
                
                credentialObj.put("category", credential.category)
                credentialObj.put("tags", credential.tags)
                credentialObj.put("dateCreated", credential.dateCreated)
                credentialObj.put("lastUpdated", credential.lastUpdated)
                credentialObj.put("isFavorite", credential.isFavorite)
                credentialsArray.put(credentialObj)
            }
            
            exportData.put("credentials", credentialsArray)
            
            val finalData = when (exportType) {
                ExportType.ENCRYPTED -> {
                    // Encrypt the entire JSON string for secure export
                    securityManager.encryptData(exportData.toString())
                }
                ExportType.PLAIN_JSON -> {
                    // Keep as plain JSON
                    exportData.toString(2) // Pretty print with 2-space indentation
                }
            }
            
            FileOutputStream(file).use { outputStream ->
                outputStream.write(finalData.toByteArray(Charsets.UTF_8))
            }
            
            val exportTypeText = if (exportType == ExportType.ENCRYPTED) "encrypted" else "plain JSON"
            _uiState.value = SettingsUiState.Success("Data exported successfully as $exportTypeText")
            
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Failed to export data: ${e.message}")
        }
    }
    
    suspend fun importData(uri: Uri) {
        try {
            _uiState.value = SettingsUiState.Loading

            val inputStream = context.contentResolver.openInputStream(uri)
            val fileContent = inputStream?.bufferedReader().use { it?.readText() }

            if (fileContent.isNullOrEmpty()) {
                _uiState.value = SettingsUiState.Error("Import failed: Could not read file or file is empty.")
                return
            }

            var jsonData: JSONObject? = null

            // First, try to decrypt the file content
            try {
                val decryptedJsonString = securityManager.decryptData(fileContent)
                if (!decryptedJsonString.isNullOrEmpty()) {
                    jsonData = JSONObject(decryptedJsonString)
                }
            } catch (e: Exception) {
                // Decryption failed, so let's try to parse it as plain JSON
                try {
                    jsonData = JSONObject(fileContent)
                } catch (jsonException: Exception) {
                    _uiState.value = SettingsUiState.Error("Import failed: File is not a valid encrypted or plain JSON file.")
                    return
                }
            }

            if (jsonData == null) {
                _uiState.value = SettingsUiState.Error("Import failed: Could not parse JSON data.")
                return
            }

            parseAndInsertCredentials(jsonData)

        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Import failed: ${e.message}")
        }
    }

    private suspend fun parseAndInsertCredentials(jsonData: JSONObject) {
        val credentialsArray = jsonData.getJSONArray("credentials")

        var importedCount = 0
        var duplicateCount = 0
        for (i in 0 until credentialsArray.length()) {
            val credentialObj = credentialsArray.getJSONObject(i)

            // Handle both new format and legacy format
            val credentialType = if (credentialObj.has("credentialType")) {
                // New format with credentialType field
                CredentialType.fromString(credentialObj.getString("credentialType"))
            } else {
                // Legacy format - assume LOGIN type
                CredentialType.LOGIN
            }

            val encryptedData = if (credentialObj.has("encryptedData")) {
                // New format with encryptedData field (encrypted export)
                credentialObj.getString("encryptedData")
            } else if (credentialObj.has("decryptedData")) {
                // New format with decryptedData field (plain JSON export) - need to encrypt it
                val decryptedJson = credentialObj.getJSONObject("decryptedData")
                securityManager.encryptData(decryptedJson.toString())
            } else {
                // Legacy format with encryptedPassword field
                credentialObj.optString("encryptedPassword", "")
            }

            val title = if (credentialObj.has("title")) {
                // New format with title field
                credentialObj.getString("title")
            } else {
                // Legacy format with websiteName field
                credentialObj.optString("websiteName", "")
            }

            val credential = com.example.aeropass.data.model.Credential(
                title = title,
                encryptedData = encryptedData,
                category = credentialObj.optString("category", "General"),
                tags = credentialObj.optString("tags", ""),
                credentialType = credentialType,
                isFavorite = credentialObj.optBoolean("isFavorite", false),
                dateCreated = credentialObj.optLong("dateCreated", System.currentTimeMillis()),
                lastUpdated = credentialObj.optLong("lastUpdated", System.currentTimeMillis())
            )

            val result = repository.insertCredentialWithDuplicateCheck(credential)
            result.onSuccess {
                importedCount++
            }.onFailure {
                if (it is com.example.aeropass.data.repository.DuplicateCredentialException) {
                    duplicateCount++
                } else {
                    _uiState.value = SettingsUiState.Error("Import failed: ${it.message}")
                    return
                }
            }
        }

        val message = "Successfully imported $importedCount credentials."
        if (duplicateCount > 0) {
            _uiState.value = SettingsUiState.Success("$message $duplicateCount duplicates were not imported.")
        } else {
            _uiState.value = SettingsUiState.Success(message)
        }
    }
    
    fun getThemeMode(): Int {
        return sessionManager.themeMode
    }
    
    fun setThemeMode(themeMode: Int) {
        sessionManager.themeMode = themeMode
    }

    fun logout() {
        sessionManager.logout()
    }

    enum class ExportType {
        ENCRYPTED,
        PLAIN_JSON
    }
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

class SettingsViewModelFactory(
    private val securityManager: SecurityManager,
    private val sessionManager: SessionManager,
    private val credentialRepository: CredentialRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(securityManager, sessionManager, credentialRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}