package com.example.qrauthscanner.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qrauthscanner.databinding.ActivityMainBinding
import com.example.qrauthscanner.security.TokenManager

/**
 * Main activity - entry point for the application
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        tokenManager = TokenManager(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.apply {
            scanQrButton.setOnClickListener {
                startScanner()
            }
            
            logoutButton.setOnClickListener {
                logout()
            }
            
            // Update UI based on authentication state
            updateAuthStatus()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateAuthStatus()
    }
    
    private fun updateAuthStatus() {
        val isAuthenticated = tokenManager.hasValidToken()
        
        binding.apply {
            if (isAuthenticated) {
                statusText.text = "Authenticated"
                statusText.setTextColor(android.graphics.Color.GREEN)
                logoutButton.visibility = android.view.View.VISIBLE
            } else {
                statusText.text = "Not authenticated"
                statusText.setTextColor(android.graphics.Color.RED)
                logoutButton.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun startScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        startActivity(intent)
    }
    
    private fun logout() {
        tokenManager.clearTokens()
        updateAuthStatus()
        android.widget.Toast.makeText(
            this,
            "Logged out successfully",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
