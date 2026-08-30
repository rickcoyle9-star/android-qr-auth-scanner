package com.example.qrauthscanner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.qrauthscanner.R
import com.example.qrauthscanner.data.QRCodeData
import com.example.qrauthscanner.databinding.ActivityScannerBinding
import com.example.qrauthscanner.security.TokenManager
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.gson.Gson
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity for scanning QR codes using ML Kit and CameraX
 */
class ScannerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityScannerBinding
    private lateinit var tokenManager: TokenManager
    private val gson = Gson()
    private var isProcessing = false
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scope = MainScope()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        tokenManager = TokenManager(this)
        
        // Request camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                
                // Preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Setup ML Kit barcode scanning
                setupBarcodeScanning(cameraProvider, preview, cameraSelector)
                
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    this,
                    "Error starting camera: ${exc.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun setupBarcodeScanning(
        cameraProvider: ProcessCameraProvider,
        preview: Preview,
        cameraSelector: CameraSelector
    ) {
        val cameraSourceConfig = CameraSourceConfig.Builder(
            this,
            BarcodeScanning.getClient(),
            { barcodes ->
                onQRCodeScanned(barcodes)
            }
        ).setFacingDirection(CameraSourceConfig.CAMERA_FACING_BACK).build()
        
        val cameraXSource = CameraXSource(cameraSourceConfig, cameraExecutor)
        
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, cameraXSource)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error binding camera: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun onQRCodeScanned(barcodes: List<Barcode>) {
        if (isProcessing || barcodes.isEmpty()) return
        
        isProcessing = true
        
        val barcode = barcodes.firstOrNull()
        val qrValue = barcode?.rawValue ?: return
        
        try {
            // Parse QR code data
            val qrData = gson.fromJson(qrValue, QRCodeData::class.java)
            
            // Validate QR code data
            if (!qrData.isValid()) {
                showError("Invalid or expired QR code")
                isProcessing = false
                return
            }
            
            // Check for replay attacks
            if (!tokenManager.isNonceValid(qrData.nonce)) {
                showError("Potential replay attack detected")
                isProcessing = false
                return
            }
            
            // Save tokens and proceed to resource access
            tokenManager.saveTokens(qrData.token, qrData = qrData)
            
            // Navigate to resource access activity
            val intent = Intent(this, ResourceAccessActivity::class.java).apply {
                putExtra("qr_data", qrData)
                putExtra("token", qrData.token)
            }
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            showError("Failed to parse QR code: ${e.message}")
            isProcessing = false
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
