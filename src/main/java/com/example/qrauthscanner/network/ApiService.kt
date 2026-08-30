package com.example.qrauthscanner.network

import com.example.qrauthscanner.data.QRCodeData
import com.example.qrauthscanner.data.ResourceAccessResponse
import com.example.qrauthscanner.data.TokenValidationResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * API Service interface for authentication and resource access
 */
interface ApiService {
    
    /**
     * Validate QR code token with backend
     */
    @POST("/api/v1/auth/validate-token")
    suspend fun validateToken(
        @Body qrCodeData: QRCodeData
    ): TokenValidationResponse
    
    /**
     * Get access to protected resource
     */
    @GET("/api/v1/resources/{resourceId}")
    suspend fun accessResource(
        @Path("resourceId") resourceId: String,
        @Header("Authorization") token: String
    ): ResourceAccessResponse
    
    /**
     * Refresh expired token
     */
    @POST("/api/v1/auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): TokenValidationResponse
    
    /**
     * Log authentication event
     */
    @POST("/api/v1/auth/log-event")
    suspend fun logAuthEvent(
        @Body authEvent: AuthEventLog
    ): Result
    
    companion object {
        private const val BASE_URL = "https://api.example.com/" // Replace with your backend URL
        private const val TIMEOUT_SECONDS = 30L
        
        fun create(): ApiService {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .addInterceptor(SecurityInterceptor()) // SSL pinning interceptor
                .build()
            
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

/**
 * Authentication event log for audit trail
 */
data class AuthEventLog(
    val userId: String,
    val resourceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val success: Boolean,
    val reason: String? = null
)

/**
 * Generic result wrapper
 */
data class Result(
    val success: Boolean,
    val message: String
)
