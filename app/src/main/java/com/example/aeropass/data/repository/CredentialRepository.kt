package com.example.aeropass.data.repository

import com.example.aeropass.data.dao.CredentialDao
import com.example.aeropass.data.model.Credential
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.utils.SecurityManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class CredentialRepository(
    private val credentialDao: CredentialDao,
    private val securityManager: SecurityManager
) {
    
    fun getAllCredentials(): Flow<List<Credential>> = credentialDao.getAllCredentials()
    
    suspend fun getCredentialById(id: Long): Credential? = credentialDao.getCredentialById(id)
    
    fun searchCredentials(query: String): Flow<List<Credential>> = credentialDao.searchCredentials(query)
    
    fun getCredentialsByCategory(category: String): Flow<List<Credential>> = 
        credentialDao.getCredentialsByCategory(category)
    
    fun getCredentialsByType(type: CredentialType): Flow<List<Credential>> = 
        credentialDao.getCredentialsByType(type)
    

    
    fun getAllCategories(): Flow<List<String>> = credentialDao.getAllCategories()
    
    suspend fun insertCredential(credential: Credential): Long = credentialDao.insertCredential(credential)
    
    suspend fun updateCredential(credential: Credential) = credentialDao.updateCredential(credential)
    
    suspend fun deleteCredential(credential: Credential) = credentialDao.deleteCredential(credential)
    
    suspend fun deleteCredentialById(id: Long) = credentialDao.deleteCredentialById(id)
    
    suspend fun deleteAllCredentials() = credentialDao.deleteAllCredentials()
    
    suspend fun getCredentialCount(): Int = credentialDao.getCredentialCount()
    
    suspend fun findDuplicateCredential(title: String, type: CredentialType, excludeId: Long = -1): List<Credential> = 
        credentialDao.findDuplicateCredential(title, type, excludeId)
    
    suspend fun hasDuplicateCredential(title: String, type: CredentialType, excludeId: Long = -1): Boolean = 
        credentialDao.countDuplicateCredentials(title, type, excludeId) > 0
    
    suspend fun insertCredentialWithDuplicateCheck(credential: Credential): Result<Long> {
        return try {
            val duplicateCredential = findCompleteDataDuplicate(credential)
            if (duplicateCredential != null) {
                Result.failure(DuplicateCredentialException(duplicateCredential.id.toString()))
            } else {
                val id = insertCredential(credential)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateCredentialWithDuplicateCheck(credential: Credential): Result<Unit> {
        return try {
            val duplicateCredential = findCompleteDataDuplicate(credential)
            if (duplicateCredential != null) {
                Result.failure(DuplicateCredentialException(duplicateCredential.id.toString()))
            } else {
                updateCredential(credential)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun findCompleteDataDuplicate(credential: Credential): Credential? {
        val existingCredentials = credentialDao.findCredentialsByType(credential.credentialType, credential.id)
        
        for (existing in existingCredentials) {
            if (areCredentialsIdentical(credential, existing)) {
                return existing
            }
        }
        return null
    }
    
    private fun areCredentialsIdentical(credential1: Credential, credential2: Credential): Boolean {
        // First check basic fields
        if (credential1.title != credential2.title ||
            credential1.credentialType != credential2.credentialType ||
            credential1.category != credential2.category ||
            credential1.tags != credential2.tags ||
            credential1.isFavorite != credential2.isFavorite) {
            return false
        }
        
        // Compare encrypted data by decrypting and comparing relevant fields
        return try {
            val data1 = if (credential1.encryptedData.isNotEmpty()) {
                JSONObject(securityManager.decryptData(credential1.encryptedData))
            } else {
                JSONObject()
            }
            
            val data2 = if (credential2.encryptedData.isNotEmpty()) {
                JSONObject(securityManager.decryptData(credential2.encryptedData))
            } else {
                JSONObject()
            }
            
            when (credential1.credentialType) {
                CredentialType.LOGIN -> compareLoginData(data1, data2)
                CredentialType.PAYMENT -> comparePaymentData(data1, data2)
                CredentialType.IDENTITY -> compareIdentityData(data1, data2)
                CredentialType.SECURE_NOTES -> compareSecureNotesData(data1, data2)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun compareLoginData(data1: JSONObject, data2: JSONObject): Boolean {
        return data1.optString("username", "") == data2.optString("username", "") &&
               data1.optString("email", "") == data2.optString("email", "") &&
               data1.optString("password", "") == data2.optString("password", "") &&
               data1.optString("url", "") == data2.optString("url", "") &&
               data1.optString("recoveryPhone", "") == data2.optString("recoveryPhone", "") &&
               data1.optString("recoveryInfo", "") == data2.optString("recoveryInfo", "") &&
               data1.optString("notes", "") == data2.optString("notes", "")
    }
    
    private fun comparePaymentData(data1: JSONObject, data2: JSONObject): Boolean {
        return data1.optString("paymentType", "") == data2.optString("paymentType", "") &&
               data1.optString("bankName", "") == data2.optString("bankName", "") &&
               data1.optString("cardholderName", "") == data2.optString("cardholderName", "") &&
               data1.optString("accountNumber", "") == data2.optString("accountNumber", "") &&
               data1.optString("cardNumber", "") == data2.optString("cardNumber", "") &&
               data1.optString("expirationDate", "") == data2.optString("expirationDate", "") &&
               data1.optString("cvv", "") == data2.optString("cvv", "") &&
               data1.optString("cardType", "") == data2.optString("cardType", "") &&
               data1.optString("routingNumber", "") == data2.optString("routingNumber", "") &&
               data1.optString("branch", "") == data2.optString("branch", "") &&
               data1.optString("internetBankingUsername", "") == data2.optString("internetBankingUsername", "") &&
               data1.optString("internetBankingPassword", "") == data2.optString("internetBankingPassword", "") &&
               data1.optString("billingAddress", "") == data2.optString("billingAddress", "") &&
               data1.optString("notes", "") == data2.optString("notes", "")
    }
    
    private fun compareIdentityData(data1: JSONObject, data2: JSONObject): Boolean {
        return data1.optString("fullName", "") == data2.optString("fullName", "") &&
               data1.optString("fathersName", "") == data2.optString("fathersName", "") &&
               data1.optString("mothersName", "") == data2.optString("mothersName", "") &&
               data1.optString("dateOfBirth", "") == data2.optString("dateOfBirth", "") &&
               data1.optString("documentType", "") == data2.optString("documentType", "") &&
               data1.optString("documentNumber", "") == data2.optString("documentNumber", "") &&
               data1.optString("nationalId", "") == data2.optString("nationalId", "") &&
               data1.optString("passportNumber", "") == data2.optString("passportNumber", "") &&
               data1.optString("drivingLicenseNumber", "") == data2.optString("drivingLicenseNumber", "") &&
               data1.optString("birthCertificateNumber", "") == data2.optString("birthCertificateNumber", "") &&
               data1.optString("dateOfIssue", "") == data2.optString("dateOfIssue", "") &&
               data1.optString("dateOfExpiry", "") == data2.optString("dateOfExpiry", "") &&
               data1.optString("validity", "") == data2.optString("validity", "") &&
               data1.optString("issuingAuthority", "") == data2.optString("issuingAuthority", "") &&
               data1.optString("address", "") == data2.optString("address", "") &&
               data1.optString("webPortal", "") == data2.optString("webPortal", "") &&
               data1.optString("notes", "") == data2.optString("notes", "")
    }
    
    private fun compareSecureNotesData(data1: JSONObject, data2: JSONObject): Boolean {
        return data1.optString("secretKey", "") == data2.optString("secretKey", "") &&
               data1.optString("qrCodeData", "") == data2.optString("qrCodeData", "") &&
               data1.optString("noteContent", "") == data2.optString("noteContent", "") &&
               data1.optString("notes", "") == data2.optString("notes", "")
    }
}

class DuplicateCredentialException(message: String) : Exception(message)