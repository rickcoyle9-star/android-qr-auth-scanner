package com.example.qrauthscanner.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.qrauthscanner.data.QRCodeData
import com.google.gson.Gson
import java.util.*

/**
 * Manages secure token storage and validation
 */
class TokenManager(context: Context) {
    
    private val gson = Gson()
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            "qr_auth_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_QR_DATA = "qr_data"
        private const val KEY_NONCE_CACHE = "nonce_cache"
        private const val TOKEN_EXPIRY_BUFFER = 300000 // 5 minutes in milliseconds
    }
    
    /**
     * Save tokens securely after QR code validation
     */
    fun saveTokens(accessToken: String, refreshToken: String? = null, qrData: QRCodeData? = null) {
        encryptedSharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            qrData?.let { putString(KEY_QR_DATA, gson.toJson(it)) }
            apply()
        }
    }
    
    /**
     * Retrieve stored access token
     */
    fun getAccessToken(): String? {
        return encryptedSharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Retrieve stored refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Retrieve stored QR code data
     */
    fun getQRData(): QRCodeData? {
        val json = encryptedSharedPreferences.getString(KEY_QR_DATA, null) ?: return null
        return try {
            gson.fromJson(json, QRCodeData::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validate JWT token
     */
    fun validateToken(token: String, secret: String? = null): Boolean {
        return try {
            val decodedJWT = JWT.decode(token)
            val expiresAt = decodedJWT.expiresAt ?: return false
            
            // Check if token is expired (with buffer)
            val currentTime = Date()
            currentTime.before(Date(expiresAt.time + TOKEN_EXPIRY_BUFFER))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check for replay attacks using nonce
     */
    fun isNonceValid(nonce: String): Boolean {
        val cachedNonces = encryptedSharedPreferences.getStringSet(KEY_NONCE_CACHE, mutableSetOf()) ?: mutableSetOf()
        
        if (nonce in cachedNonces) {
            return false // Nonce already used - potential replay attack
        }
        
        // Add nonce to cache
        cachedNonces.add(nonce)
        encryptedSharedPreferences.edit().putStringSet(KEY_NONCE_CACHE, cachedNonces).apply()
        
        return true
    }
    
    /**
     * Clear all stored tokens (logout)
     */
    fun clearTokens() {
        encryptedSharedPreferences.edit().clear().apply()
    }
    
    /**
     * Check if user has valid tokens
     */
    fun hasValidToken(): Boolean {
        val token = getAccessToken() ?: return false
        return validateToken(token)
    }
}
