package com.example.aeropass.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.data.model.Credential
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.data.model.DecryptedCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as AeroPassApplication
    private val repository = app.repository
    private val securityManager = app.securityManager
    
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    
    val credentials = combine(
        repository.getAllCredentials(),
        _searchQuery,
        _selectedCategory
    ) { credentials, query, category ->
        var filtered = credentials
        
        if (category != "All") {
            filtered = filtered.filter { it.category == category }
        }
        
        if (query.isNotBlank()) {
            filtered = filtered.filter { credential ->
                // Search in basic fields
                credential.title.contains(query, ignoreCase = true) ||
                credential.tags.contains(query, ignoreCase = true) ||
                credential.category.contains(query, ignoreCase = true) ||
                
                // Search in decrypted content for more comprehensive results
                searchInDecryptedContent(credential, query)
            }
        }
        
        filtered
    }.asLiveData()
    
    val categories = repository.getAllCategories().asLiveData()
    
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    
    fun searchCredentials(query: String) {
        _searchQuery.value = query
    }
    
    fun filterByCategory(category: String) {
        _selectedCategory.value = category
    }
    
    private fun searchInDecryptedContent(credential: Credential, query: String): Boolean {
        return try {
            val decryptedCredential = decryptCredential(credential)
            decryptedCredential?.let { decrypted ->
                when (decrypted.type) {
                    CredentialType.LOGIN -> {
                        decrypted.username.contains(query, ignoreCase = true) ||
                        decrypted.email.contains(query, ignoreCase = true) ||
                        decrypted.url.contains(query, ignoreCase = true) ||
                        decrypted.recoveryPhone.contains(query, ignoreCase = true) ||
                        decrypted.notes.contains(query, ignoreCase = true)
                    }
                    CredentialType.PAYMENT -> {
                        decrypted.cardholderName.contains(query, ignoreCase = true) ||
                        decrypted.bankName.contains(query, ignoreCase = true) ||
                        decrypted.cardType.contains(query, ignoreCase = true) ||
                        decrypted.paymentType.contains(query, ignoreCase = true) ||
                        decrypted.branch.contains(query, ignoreCase = true)
                    }
                    CredentialType.IDENTITY -> {
                        decrypted.fullName.contains(query, ignoreCase = true) ||
                        decrypted.fathersName.contains(query, ignoreCase = true) ||
                        decrypted.mothersName.contains(query, ignoreCase = true) ||
                        decrypted.documentType.contains(query, ignoreCase = true) ||
                        decrypted.address.contains(query, ignoreCase = true) ||
                        decrypted.issuingAuthority.contains(query, ignoreCase = true)
                    }
                    CredentialType.SECURE_NOTES -> {
                        decrypted.noteContent.contains(query, ignoreCase = true)
                    }
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteCredential(credential: Credential) {
        viewModelScope.launch {
            try {
                repository.deleteCredential(credential)
                _uiState.value = UiState.Success("Credential deleted")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete credential: ${e.message}")
            }
        }
    }
    
    fun decryptCredential(credential: Credential): DecryptedCredential? {
        return try {
            val decryptedData = if (credential.encryptedData.isNotEmpty()) {
                securityManager.decryptData(credential.encryptedData)
            } else {
                "{}"
            }
            
            val jsonData = JSONObject(decryptedData)
            
            DecryptedCredential(
                id = credential.id,
                title = credential.title,
                category = credential.category,
                tags = if (credential.tags.isNotEmpty()) credential.tags.split(",").map { it.trim() } else emptyList(),
                type = credential.credentialType,
                dateCreated = credential.dateCreated,
                lastUpdated = credential.lastUpdated,
                isFavorite = credential.isFavorite,
                createdAt = credential.dateCreated,
                updatedAt = credential.lastUpdated,
                
                // Decrypt specific fields
                username = jsonData.optString("username", ""),
                email = jsonData.optString("email", ""),
                password = jsonData.optString("password", ""),
                url = jsonData.optString("url", ""),
                recoveryInfo = jsonData.optString("recoveryInfo", ""),
                notes = jsonData.optString("notes", ""),
                
                // Payment fields
                bankName = jsonData.optString("bankName", ""),
                cardholderName = jsonData.optString("cardholderName", ""),
                accountNumber = jsonData.optString("accountNumber", ""),
                cardNumber = jsonData.optString("cardNumber", ""),
                expirationDate = jsonData.optString("expirationDate", ""),
                cvv = jsonData.optString("cvv", ""),
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

                webPortal = jsonData.optString("webPortal", ""),
                
                // Secure notes fields
                secretKey = jsonData.optString("secretKey", ""),
                qrCodeData = jsonData.optString("qrCodeData", ""),
                noteContent = jsonData.optString("noteContent", "")
            )
        } catch (e: Exception) {
            // Fallback for credentials without encrypted data or with old format
            try {
                DecryptedCredential(
                    id = credential.id,
                    title = credential.title,
                    category = credential.category,
                    tags = if (credential.tags.isNotEmpty()) credential.tags.split(",").map { it.trim() } else emptyList(),
                    type = credential.credentialType,
                    dateCreated = credential.dateCreated,
                    lastUpdated = credential.lastUpdated,
                    isFavorite = credential.isFavorite,
                    createdAt = credential.dateCreated,
                    updatedAt = credential.lastUpdated
                )
            } catch (ex: Exception) {
                _uiState.value = UiState.Error("Failed to decrypt credential: ${ex.message}")
                null
            }
        }
    }
    
    sealed class UiState {
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}