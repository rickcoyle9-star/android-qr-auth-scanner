package com.example.qrauthscanner.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data class representing the structure of a QR code used for authentication.
 * QR codes should encode JSON in this format.
 */
data class QRCodeData(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("token")
    val token: String,
    
    @SerializedName("resource_id")
    val resourceId: String,
    
    @SerializedName("expiry")
    val expiry: Long, // Unix timestamp in milliseconds
    
    @SerializedName("permissions")
    val permissions: List<String> = listOf("read", "write"),
    
    @SerializedName("nonce")
    val nonce: String // For replay attack prevention
) : Serializable {
    
    /**
     * Check if the token has expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiry
    }
    
    /**
     * Check if required fields are present
     */
    fun isValid(): Boolean {
        return userId.isNotBlank() && 
               token.isNotBlank() && 
               resourceId.isNotBlank() &&
               nonce.isNotBlank() &&
               !isExpired()
    }
}

/**
 * API response for token validation
 */
data class TokenValidationResponse(
    @SerializedName("valid")
    val valid: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("access_token")
    val accessToken: String? = null,
    
    @SerializedName("resource_url")
    val resourceUrl: String? = null
)

/**
 * Resource access response after successful authentication
 */
data class ResourceAccessResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: Map<String, String>? = null,
    
    @SerializedName("error")
    val error: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
