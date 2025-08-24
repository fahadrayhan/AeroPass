package com.example.aeropass.data.model

import androidx.annotation.DrawableRes
import com.example.aeropass.R

enum class CredentialType(
    val displayName: String,
    val description: String,
    @DrawableRes val icon: Int
) {
    LOGIN(
        "Login Credential",
        "Store website or app login details",
        R.drawable.ic_lock
    ),
    PAYMENT(
        "Payment Information",
        "Store credit/debit card details securely",
        R.drawable.ic_save
    ),
    IDENTITY(
        "Identity Document",
        "Store personal identification documents",
        R.drawable.ic_person
    ),
    SECURE_NOTES(
        "Secure Notes",
        "Store sensitive information and 2FA tokens",
        R.drawable.ic_note
    );
    
    companion object {
        fun fromString(type: String): CredentialType {
            return values().find { it.name == type } ?: LOGIN
        }
        
        fun getAllTypes(): List<CredentialType> {
            return values().toList()
        }
    }
}