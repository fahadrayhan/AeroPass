package com.example.aeropass.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.aeropass.data.model.Credential
import com.example.aeropass.data.model.CredentialType
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    
    @Query("SELECT * FROM credentials ORDER BY lastUpdated DESC")
    fun getAllCredentials(): Flow<List<Credential>>
    
    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getCredentialById(id: Long): Credential?

    @Query("SELECT * FROM credentials WHERE title = :title")
    suspend fun getCredentialByTitle(title: String): Credential?
    
    @Query("SELECT * FROM credentials WHERE title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY lastUpdated DESC")
    fun searchCredentials(query: String): Flow<List<Credential>>
    
    @Query("SELECT * FROM credentials WHERE category = :category ORDER BY lastUpdated DESC")
    fun getCredentialsByCategory(category: String): Flow<List<Credential>>
    
    @Query("SELECT * FROM credentials WHERE credentialType = :type ORDER BY lastUpdated DESC")
    fun getCredentialsByType(type: CredentialType): Flow<List<Credential>>
    

    
    @Query("SELECT DISTINCT category FROM credentials ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCredential(credential: Credential): Long
    
    @Update
    suspend fun updateCredential(credential: Credential)
    
    @Delete
    suspend fun deleteCredential(credential: Credential)
    
    @Query("DELETE FROM credentials WHERE id = :id")
    suspend fun deleteCredentialById(id: Long)
    
    @Query("DELETE FROM credentials")
    suspend fun deleteAllCredentials()
    
    @Query("SELECT COUNT(*) FROM credentials")
    suspend fun getCredentialCount(): Int
    
    @Query("SELECT * FROM credentials WHERE title = :title AND credentialType = :type AND id != :excludeId")
    suspend fun findDuplicateCredential(title: String, type: CredentialType, excludeId: Long = -1): List<Credential>
    
    @Query("SELECT COUNT(*) FROM credentials WHERE title = :title AND credentialType = :type AND id != :excludeId")
    suspend fun countDuplicateCredentials(title: String, type: CredentialType, excludeId: Long = -1): Int
    
    @Query("SELECT * FROM credentials WHERE credentialType = :type AND id != :excludeId")
    suspend fun findCredentialsByType(type: CredentialType, excludeId: Long = -1): List<Credential>
}