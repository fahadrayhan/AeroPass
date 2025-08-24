package com.example.aeropass.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.security.SecureRandom

class SecurityManager(private val context: Context) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val KEY_ALIAS = "AeroPassMasterKey"
        private const val PREFS_NAME = "aeropass_secure_prefs"
        private const val MASTER_PASSWORD_KEY = "master_password_hash"
        private const val SALT_KEY = "password_salt"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    init {
        generateOrGetSecretKey()
    }
    
    private fun generateOrGetSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    fun encryptData(data: String): String {
        try {
            val secretKey = generateOrGetSecretKey()
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            val combined = iv + encryptedData
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }
    
    fun decryptData(encryptedData: String): String {
        try {
            val secretKey = generateOrGetSecretKey()
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            val iv = combined.sliceArray(0..11) // GCM IV is 12 bytes
            val cipherText = combined.sliceArray(12 until combined.size)
            
            val cipher = Cipher.getInstance(AES_MODE)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedData = cipher.doFinal(cipherText)
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }
    
    fun hashPassword(password: String, salt: ByteArray? = null): Pair<String, ByteArray> {
        val actualSalt = salt ?: generateSalt()
        val hashedPassword = hashWithSalt(password, actualSalt)
        return Pair(hashedPassword, actualSalt)
    }
    
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    private fun hashWithSalt(password: String, salt: ByteArray): String {
        val combined = password.toByteArray() + salt
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined)
        return Base64.encodeToString(hash, Base64.DEFAULT)
    }
    
    fun saveMasterPassword(password: String) {
        val (hashedPassword, salt) = hashPassword(password)
        encryptedPrefs.edit()
            .putString(MASTER_PASSWORD_KEY, hashedPassword)
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.DEFAULT))
            .apply()
    }
    
    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = encryptedPrefs.getString(MASTER_PASSWORD_KEY, null) ?: return false
        val storedSalt = encryptedPrefs.getString(SALT_KEY, null)?.let {
            Base64.decode(it, Base64.DEFAULT)
        } ?: return false
        
        val (inputHash, _) = hashPassword(password, storedSalt)
        return inputHash == storedHash
    }
    
    fun isMasterPasswordSet(): Boolean {
        return encryptedPrefs.contains(MASTER_PASSWORD_KEY)
    }
    
    fun changeMasterPassword(currentPassword: String, newPassword: String) {
        // Verify the current password first
        if (!verifyMasterPassword(currentPassword)) {
            throw SecurityException("Current password is incorrect")
        }
        
        // Save the new password
        saveMasterPassword(newPassword)
    }
    
    fun clearAllData() {
        encryptedPrefs.edit().clear().apply()
        
        // Delete the key from Android Keystore
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Handle error
        }
    }
}