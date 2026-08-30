# Setup Guide - Android QR Code Authentication Scanner

## Step 1: Project Structure Setup

The project uses the following structure:

```
project/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/qrauthscanner/
│   │   │   │   ├── ui/
│   │   │   │   ├── data/
│   │   │   │   ├── security/
│   │   │   │   └── network/
│   │   │   └── res/
│   │   │       └── layout/
│   │   └── test/
│   ├── build.gradle.kts
│   └── AndroidManifest.xml
├── build.gradle.kts (project-level)
└── settings.gradle.kts
```

## Step 2: Backend API Configuration

### Update API Base URL

Edit `src/main/java/com/example/qrauthscanner/network/ApiService.kt`:

```kotlin
companion object {
    private const val BASE_URL = "https://your-api-endpoint.com/" // Update this
    private const val TIMEOUT_SECONDS = 30L
    // ... rest of code
}
```

## Step 3: Configure SSL Certificate Pinning (Production)

### For Production Deployments:

1. Get your backend's SSL certificate:
```bash
openssl s_client -connect api.example.com:443 -showcerts
```

2. Save the certificate to `res/raw/certificates.pem`

3. Update `SecurityInterceptor.kt` to use the certificate:
```kotlin
val certificatePem = context.resources.openRawResource(R.raw.certificates).bufferedReader().use { it.readText() }
val sslContext = SecurityInterceptor.createPinnedSSLContext(certificatePem)
```

## Step 4: Dependencies Installation

All dependencies are defined in `build.gradle.kts`. To install:

```bash
# Using Android Studio
# File > Sync Now

# Or command line
./gradlew assembleDebug
```

### Key Dependencies:
- ML Kit Vision: QR code scanning
- CameraX: Camera access
- Retrofit: API client
- Android Keystore: Token encryption

## Step 5: Manifest Configuration

Permissions are already configured in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Step 6: Runtime Permissions

The app requests camera permission at runtime. For Android 6.0+, users must grant permission.

## Step 7: Build Configuration

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
# Configure signing in build.gradle.kts first
./gradlew assembleRelease
```

### ProGuard Rules

Add to `proguard-rules.pro` for release builds:

```proguard
# Keep Retrofit models
-keep class com.example.qrauthscanner.data.** { *; }
-keep class com.example.qrauthscanner.network.** { *; }

# Keep JWT library
-keep class com.auth0.jwt.** { *; }

# Keep Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
```

## Step 8: Testing

### Unit Tests
```bash
./gradlew testDebug
```

### Instrumented Tests (on device)
```bash
./gradlew connectedAndroidTest
```

### Manual Testing

1. Generate a test QR code:
```python
import qrcode, json

data = {
    "user_id": "test_user",
    "token": "test_token_value",
    "resource_id": "test_resource",
    "expiry": 9999999999000,
    "permissions": ["read"],
    "nonce": "nonce_123"
}

qr = qrcode.QRCode()
qr.add_data(json.dumps(data))
qr.make()
qr.make_image().save("test.png")
```

2. Run the app on a device/emulator
3. Tap "Scan QR Code"
4. Point camera at generated QR code
5. Verify success message

## Step 9: Environment Configuration

### Development
```kotlin
private const val BASE_URL = "http://localhost:3000/" // Local dev server
```

### Staging
```kotlin
private const val BASE_URL = "https://staging-api.example.com/"
```

### Production
```kotlin
private const val BASE_URL = "https://api.example.com/"
```

## Step 10: Debugging

### Enable Logging

Update `ApiService.kt` for detailed logs:
```kotlin
.addInterceptor(HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY // Shows request/response
})
```

### Logcat Filtering
```bash
# Filter QR auth logs
adb logcat | grep qrauthscanner

# All logs
adb logcat
```

## Step 11: Security Checklist

- [ ] Update API base URL to production endpoint
- [ ] Enable SSL certificate pinning
- [ ] Configure ProGuard/R8 for release
- [ ] Enable app signing
- [ ] Test token expiration
- [ ] Verify nonce validation
- [ ] Test error handling
- [ ] Validate JWT tokens on backend

## Common Issues

### Issue: Build fails with "Cannot find symbol"
**Solution:** Run `./gradlew clean && ./gradlew assembleDebug`

### Issue: Camera permission denied
**Solution:** Grant permission in app settings > Permissions > Camera

### Issue: API connection timeout
**Solution:** 
- Verify BASE_URL is correct
- Check network connectivity
- Verify backend is running
- Check firewall settings

### Issue: QR code not detected
**Solution:**
- Ensure good lighting
- Verify QR code is valid JSON format
- Check ML Kit models are downloaded
- Update Google Play Services

## Next Steps

1. Implement your backend API server
2. Set up SSL certificates
3. Configure database for audit logs
4. Implement token refresh logic
5. Set up monitoring and alerts
6. Deploy to Play Store

## Support

For detailed API documentation, see [BACKEND.md](./BACKEND.md)
For security guidelines, see [SECURITY.md](./SECURITY.md)
