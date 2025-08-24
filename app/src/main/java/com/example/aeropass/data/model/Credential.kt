package com.example.aeropass.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "credentials")
data class Credential(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val credentialType: CredentialType = CredentialType.LOGIN,
    val title: String = "",
    val encryptedData: String = "",
    val category: String = "General",
    val tags: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) : Parcelable

@Parcelize
data class DecryptedCredential(
    val id: Long = 0,
    val title: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val type: CredentialType = CredentialType.LOGIN,
    val dateCreated: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    
    // Login credential fields
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val url: String = "",
    val recoveryPhone: String = "",
    val recoveryInfo: String = "",
    val notes: String = "",
    
    // Payment credential fields
    val paymentType: String = "", // "Bank Account" or "Credit/Debit Card"
    val bankName: String = "",
    val cardholderName: String = "", // Also used as account holder name
    val accountNumber: String = "",
    val cardNumber: String = "",
    val expirationDate: String = "",
    val cvv: String = "",
    val cardType: String = "", // Visa, MasterCard, etc.
    val routingNumber: String = "",
    val branch: String = "",
    val internetBankingUsername: String = "",
    val internetBankingPassword: String = "",
    val billingAddress: String = "",
    
    // Identity credential fields
    val fullName: String = "",
    val fathersName: String = "",
    val mothersName: String = "",
    val dateOfBirth: String = "",
    val documentType: String = "", // National ID, Passport, Driving License, Birth Certificate
    val documentNumber: String = "", // Generic document number
    val nationalId: String = "", // For National ID
    val passportNumber: String = "", // For Passport
    val drivingLicenseNumber: String = "", // For Driving License
    val birthCertificateNumber: String = "", // For Birth Certificate
    val dateOfIssue: String = "", // For Passport and Driving License
    val dateOfExpiry: String = "", // For Passport
    val validity: String = "", // For Driving License
    val issuingAuthority: String = "", // For Driving License
    val address: String = "", // For National ID and Birth Certificate
    val webPortal: String = "",
    
    // Secure notes fields
    val secretKey: String = "",
    val qrCodeData: String = "",
    val noteContent: String = "",
    
    // Additional fields for compatibility
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable