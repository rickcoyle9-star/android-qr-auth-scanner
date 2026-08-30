package com.example.qrauthscanner.network

import okhttp3.Interceptor
import okhttp3.Response
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Interceptor for SSL pinning and security headers
 */
class SecurityInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Add security headers
        val requestBuilder = originalRequest.newBuilder()
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
    
    companion object {
        /**
         * Create SSL context with certificate pinning
         * 
         * To implement certificate pinning:
         * 1. Get your backend's certificate
         * 2. Convert to X.509 format
         * 3. Add to res/raw/certificates.pem
         * 4. Use this method to enforce pinning
         */
        fun createPinnedSSLContext(certificatePem: String): SSLContext {
            return try {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                val certificate = certificateFactory.generateCertificate(
                    certificatePem.byteInputStream()
                ) as? Certificate ?: throw IllegalStateException("Failed to load certificate")
                
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                keyStore.load(null, null)
                keyStore.setCertificateEntry("pinned", certificate)
                
                val trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
                )
                trustManagerFactory.init(keyStore)
                
                val trustManagers: Array<TrustManager> = trustManagerFactory.trustManagers
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustManagers, java.security.SecureRandom())
                
                sslContext
            } catch (e: Exception) {
                throw RuntimeException("Failed to create pinned SSL context", e)
            }
        }
    }
}
