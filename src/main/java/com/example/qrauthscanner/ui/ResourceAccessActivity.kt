package com.example.qrauthscanner.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qrauthscanner.data.QRCodeData
import com.example.qrauthscanner.databinding.ActivityResourceAccessBinding
import com.example.qrauthscanner.network.ApiService
import com.example.qrauthscanner.network.AuthEventLog
import com.example.qrauthscanner.security.TokenManager
import kotlinx.coroutines.launch

/**
 * Activity for accessing protected resources after QR code authentication
 */
class ResourceAccessActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityResourceAccessBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var apiService: ApiService
    private var qrData: QRCodeData? = null
    private var token: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResourceAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        tokenManager = TokenManager(this)
        apiService = ApiService.create()
        
        // Get data from intent
        qrData = intent.getSerializableExtra("qr_data") as? QRCodeData
        token = intent.getStringExtra("token")
        
        if (qrData == null || token == null) {
            showError("Invalid authentication data")
            finish()
            return
        }
        
        setupUI()
        requestResourceAccess()
    }
    
    private fun setupUI() {
        binding.apply {
            userIdValue.text = qrData?.userId ?: "N/A"
            resourceIdValue.text = qrData?.resourceId ?: "N/A"
            permissionsValue.text = qrData?.permissions?.joinToString(", ") ?: "N/A"
            expiryValue.text = formatTimestamp(qrData?.expiry ?: 0)
            
            accessButton.setOnClickListener {
                requestResourceAccess()
            }
            
            refreshButton.setOnClickListener {
                refreshToken()
            }
        }
    }
    
    private fun requestResourceAccess() {
        if (token == null || qrData == null) {
            showError("Missing authentication token")
            return
        }
        
        lifecycleScope.launch {
            try {
                binding.progressBar.show()
                binding.accessButton.isEnabled = false
                
                // Validate token with backend first
                validateTokenWithBackend()
                
                // Access the protected resource
                val response = apiService.accessResource(
                    qrData!!.resourceId,
                    "Bearer $token"
                )
                
                if (response.success && response.data != null) {
                    displayResourceData(response.data)
                    logAuthEvent(true, null)
                } else {
                    showError("Access denied: ${response.error}")
                    logAuthEvent(false, response.error ?: "Unknown error")
                }
                
            } catch (e: Exception) {
                showError("Error accessing resource: ${e.message}")
                logAuthEvent(false, e.message)
            } finally {
                binding.progressBar.hide()
                binding.accessButton.isEnabled = true
            }
        }
    }
    
    private suspend fun validateTokenWithBackend() {
        val qrCodeData = qrData ?: return
        
        val validationResponse = apiService.validateToken(qrCodeData)
        
        if (!validationResponse.valid) {
            throw Exception("Token validation failed: ${validationResponse.message}")
        }
        
        // Update access token if new one provided
        validationResponse.accessToken?.let {
            tokenManager.saveTokens(it, qrData = qrCodeData)
        }
    }
    
    private fun displayResourceData(data: Map<String, String>) {
        val displayText = data.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }
        
        binding.resourceDataText.text = displayText
        binding.resourceDataContainer.visibility = android.view.View.VISIBLE
        
        Toast.makeText(
            this,
            "Access granted successfully!",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun refreshToken() {
        lifecycleScope.launch {
            try {
                binding.progressBar.show()
                
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken == null) {
                    showError("No refresh token available")
                    return@launch
                }
                
                val response = apiService.refreshToken("Bearer $refreshToken")
                
                if (response.valid && response.accessToken != null) {
                    tokenManager.saveTokens(response.accessToken!!)
                    token = response.accessToken
                    Toast.makeText(this@ResourceAccessActivity, "Token refreshed", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Token refresh failed: ${response.message}")
                }
                
            } catch (e: Exception) {
                showError("Error refreshing token: ${e.message}")
            } finally {
                binding.progressBar.hide()
            }
        }
    }
    
    private suspend fun logAuthEvent(success: Boolean, reason: String? = null) {
        try {
            val event = AuthEventLog(
                userId = qrData?.userId ?: "unknown",
                resourceId = qrData?.resourceId ?: "unknown",
                deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ),
                success = success,
                reason = reason
            )
            
            apiService.logAuthEvent(event)
        } catch (e: Exception) {
            // Log errors silently to not disrupt user experience
            e.printStackTrace()
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.errorMessage.apply {
            text = message
            visibility = android.view.View.VISIBLE
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}
