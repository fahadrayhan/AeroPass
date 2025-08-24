package com.example.aeropass.ui.credential

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.data.model.Credential
import com.example.aeropass.data.model.CredentialCategory
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.data.model.DecryptedCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class CredentialViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    
    private val app = application as AeroPassApplication
    private val repository = app.repository
    private val securityManager = app.securityManager
    
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    
    private val _credential = MutableLiveData<DecryptedCredential?>()
    val credential: LiveData<DecryptedCredential?> = _credential
    
    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult
    
    private val _operationResult = MutableLiveData<Result<Long>>()
    val operationResult: LiveData<Result<Long>> = _operationResult

    private val _searchResults = MutableLiveData<List<String>>()
    val searchResults: LiveData<List<String>> = _searchResults
    
    // Autofill functionality removed

    fun searchCredentials(query: String) {
        viewModelScope.launch {
            repository.searchCredentials(query).collect { credentials ->
                val suggestions = credentials.map { it.title }.distinct()
                _searchResults.postValue(suggestions)
            }
        }
    }
    
    // Autofill suggestions method removed
    
    // Smart autofill suggestions method removed
    
    // Smart filtered credentials method removed
    
    fun loadCredential(credentialId: Long) {
        if (credentialId == -1L) {
            // New credential
            _credential.value = DecryptedCredential(
                title = "",
                category = CredentialCategory.GENERAL.displayName,
                tags = emptyList(),
                type = CredentialType.LOGIN,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val encryptedCredential = repository.getCredentialById(credentialId)
                if (encryptedCredential != null) {
                    val decrypted = decryptCredentialData(encryptedCredential)
                    _credential.value = decrypted
                } else {
                    _uiState.value = UiState.Error("Credential not found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load credential: ${e.message}")
            }
        }
    }
    
    fun saveCredential(decryptedCredential: DecryptedCredential) {
        viewModelScope.launch {
            try {
                if (decryptedCredential.title.isBlank()) {
                    _operationResult.value = Result.failure(Exception("Title is required"))
                    return@launch
                }
                
                val currentTime = System.currentTimeMillis()
                val encryptedData = encryptCredentialData(decryptedCredential)
                
                val encryptedCredential = Credential(
                    id = decryptedCredential.id,
                    title = decryptedCredential.title,
                    credentialType = decryptedCredential.type,
                    encryptedData = encryptedData,
                    tags = decryptedCredential.tags.joinToString(","),
                    category = decryptedCredential.category,
                    dateCreated = if (decryptedCredential.id == 0L) currentTime else decryptedCredential.dateCreated,
                    lastUpdated = currentTime,
                    isFavorite = decryptedCredential.isFavorite
                )
                
                if (decryptedCredential.id == 0L) {
                    val result = repository.insertCredentialWithDuplicateCheck(encryptedCredential)
                    _operationResult.value = result
                } else {
                    val result = repository.updateCredentialWithDuplicateCheck(encryptedCredential)
                    if (result.isSuccess) {
                        _operationResult.value = Result.success(decryptedCredential.id)
                    } else {
                        _operationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Update failed"))
                    }
                }
            } catch (e: Exception) {
                _operationResult.value = Result.failure(e)
            }
        }
    }
    

    
    fun addCredential(decryptedCredential: DecryptedCredential) {
        saveCredential(decryptedCredential)
    }
    
    fun updateCredential(decryptedCredential: DecryptedCredential) {
        saveCredential(decryptedCredential)
    }
    
    fun deleteCredential(credentialId: Long) {
        viewModelScope.launch {
            try {
                val credential = repository.getCredentialById(credentialId)
                if (credential != null) {
                    repository.deleteCredential(credential)
                    _uiState.value = UiState.Success("Credential deleted")
                    _deleteResult.value = Result.success(Unit)
                } else {
                    _uiState.value = UiState.Error("Credential not found")
                    _deleteResult.value = Result.failure(Exception("Credential not found"))
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete credential: ${e.message}")
                _deleteResult.value = Result.failure(e)
            }
        }
    }
    
    fun getCredentialById(credentialId: String): LiveData<DecryptedCredential?> {
        val result = MutableLiveData<DecryptedCredential?>()
        viewModelScope.launch {
            try {
                val credential = repository.getCredentialById(credentialId.toLong())
                if (credential != null) {
                    val decrypted = decryptCredentialData(credential)
                    result.value = decrypted
                } else {
                    result.value = null
                }
            } catch (e: Exception) {
                result.value = null
            }
        }
        return result
    }
    
    private fun encryptCredentialData(decryptedCredential: DecryptedCredential): String {
        val jsonData = JSONObject().apply {
            // Common fields
            put("username", decryptedCredential.username)
            put("email", decryptedCredential.email)
            put("password", decryptedCredential.password)
            put("url", decryptedCredential.url)
            put("recoveryPhone", decryptedCredential.recoveryPhone)
            put("recoveryInfo", decryptedCredential.recoveryInfo)
            put("notes", decryptedCredential.notes)
            
            // Payment fields
            put("paymentType", decryptedCredential.paymentType)
            put("bankName", decryptedCredential.bankName)
            put("cardholderName", decryptedCredential.cardholderName)
            put("accountNumber", decryptedCredential.accountNumber)
            put("cardNumber", decryptedCredential.cardNumber)
            put("expirationDate", decryptedCredential.expirationDate)
            put("cvv", decryptedCredential.cvv)
            put("cardType", decryptedCredential.cardType)
            put("routingNumber", decryptedCredential.routingNumber)
            put("branch", decryptedCredential.branch)
            put("internetBankingUsername", decryptedCredential.internetBankingUsername)
            put("internetBankingPassword", decryptedCredential.internetBankingPassword)
            put("billingAddress", decryptedCredential.billingAddress)
            
            // Identity fields
            put("fullName", decryptedCredential.fullName)
            put("fathersName", decryptedCredential.fathersName)
            put("mothersName", decryptedCredential.mothersName)
            put("dateOfBirth", decryptedCredential.dateOfBirth)
            put("documentType", decryptedCredential.documentType)
            put("documentNumber", decryptedCredential.documentNumber)
            put("nationalId", decryptedCredential.nationalId)
            put("passportNumber", decryptedCredential.passportNumber)
            put("drivingLicenseNumber", decryptedCredential.drivingLicenseNumber)
            put("birthCertificateNumber", decryptedCredential.birthCertificateNumber)
            put("dateOfIssue", decryptedCredential.dateOfIssue)
            put("dateOfExpiry", decryptedCredential.dateOfExpiry)
            put("validity", decryptedCredential.validity)
            put("issuingAuthority", decryptedCredential.issuingAuthority)
            put("address", decryptedCredential.address)
            put("webPortal", decryptedCredential.webPortal)
            
            // Secure notes fields
            put("secretKey", decryptedCredential.secretKey)
            put("qrCodeData", decryptedCredential.qrCodeData)
            put("noteContent", decryptedCredential.noteContent)
        }
        
        return securityManager.encryptData(jsonData.toString())
    }
    
    private fun decryptCredentialData(encryptedCredential: Credential): DecryptedCredential {
        return try {
            val decryptedData = if (encryptedCredential.encryptedData.isNotEmpty()) {
                securityManager.decryptData(encryptedCredential.encryptedData)
            } else {
                "{}"
            }
            
            val jsonData = JSONObject(decryptedData)
            
            DecryptedCredential(
                id = encryptedCredential.id,
                title = encryptedCredential.title,
                category = encryptedCredential.category,
                tags = if (encryptedCredential.tags.isNotEmpty()) encryptedCredential.tags.split(",").map { it.trim() } else emptyList(),
                type = encryptedCredential.credentialType,
                dateCreated = encryptedCredential.dateCreated,
                lastUpdated = encryptedCredential.lastUpdated,
                isFavorite = encryptedCredential.isFavorite,
                createdAt = encryptedCredential.dateCreated,
                updatedAt = encryptedCredential.lastUpdated,
                
                // Decrypt specific fields
                username = jsonData.optString("username", ""),
                email = jsonData.optString("email", ""),
                password = jsonData.optString("password", ""),
                url = jsonData.optString("url", ""),
                recoveryPhone = jsonData.optString("recoveryPhone", ""),
                recoveryInfo = jsonData.optString("recoveryInfo", ""),
                notes = jsonData.optString("notes", ""),
                
                // Payment fields
                paymentType = jsonData.optString("paymentType", ""),
                bankName = jsonData.optString("bankName", ""),
                cardholderName = jsonData.optString("cardholderName", ""),
                accountNumber = jsonData.optString("accountNumber", ""),
                cardNumber = jsonData.optString("cardNumber", ""),
                expirationDate = jsonData.optString("expirationDate", ""),
                cvv = jsonData.optString("cvv", ""),
                cardType = jsonData.optString("cardType", ""),
                routingNumber = jsonData.optString("routingNumber", ""),
                branch = jsonData.optString("branch", ""),
                internetBankingUsername = jsonData.optString("internetBankingUsername", ""),
                internetBankingPassword = jsonData.optString("internetBankingPassword", ""),
                billingAddress = jsonData.optString("billingAddress", ""),
                
                // Identity fields
                fullName = jsonData.optString("fullName", ""),
                fathersName = jsonData.optString("fathersName", ""),
                mothersName = jsonData.optString("mothersName", ""),
                dateOfBirth = jsonData.optString("dateOfBirth", ""),
                documentType = jsonData.optString("documentType", ""),
                documentNumber = jsonData.optString("documentNumber", ""),
                nationalId = jsonData.optString("nationalId", ""),
                passportNumber = jsonData.optString("passportNumber", ""),
                drivingLicenseNumber = jsonData.optString("drivingLicenseNumber", ""),
                birthCertificateNumber = jsonData.optString("birthCertificateNumber", ""),
                dateOfIssue = jsonData.optString("dateOfIssue", ""),
                dateOfExpiry = jsonData.optString("dateOfExpiry", ""),
                validity = jsonData.optString("validity", ""),
                issuingAuthority = jsonData.optString("issuingAuthority", ""),
                address = jsonData.optString("address", ""),
                webPortal = jsonData.optString("webPortal", ""),
                
                // Secure notes fields
                secretKey = jsonData.optString("secretKey", ""),
                qrCodeData = jsonData.optString("qrCodeData", ""),
                noteContent = jsonData.optString("noteContent", "")
            )
        } catch (e: Exception) {
            android.util.Log.e("CredentialViewModel", "Error decrypting credential", e)
            // Fallback for credentials without encrypted data or with old format
            DecryptedCredential(
                id = encryptedCredential.id,
                title = encryptedCredential.title,
                category = encryptedCredential.category,
                tags = if (encryptedCredential.tags.isNotEmpty()) encryptedCredential.tags.split(",").map { it.trim() } else emptyList(),
                type = encryptedCredential.credentialType,
                dateCreated = encryptedCredential.dateCreated,
                lastUpdated = encryptedCredential.lastUpdated,
                createdAt = encryptedCredential.dateCreated,
                updatedAt = encryptedCredential.lastUpdated
            )
        }
    }
    
    sealed class UiState {
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}