# Android QR Code Authentication Scanner

A secure Android application that uses QR code scanning for authentication and grants access to protected resources.

## Features

✅ **QR Code Scanning** - Uses Google ML Kit for real-time QR code detection
✅ **JWT Token Validation** - Secure token validation and expiration checks
✅ **Encrypted Storage** - Tokens stored securely using Android Keystore
✅ **Replay Attack Prevention** - Nonce-based validation
✅ **SSL Certificate Pinning** - Secure backend communication
✅ **Resource Access Control** - Permission-based resource access
✅ **Authentication Audit Logging** - Track all authentication events

## Architecture

```
android-qr-auth-scanner/
├── src/main/java/com/example/qrauthscanner/
│   ├── ui/
│   │   ├── MainActivity.kt              # Entry point
│   │   ├── ScannerActivity.kt           # QR code scanning
│   │   └── ResourceAccessActivity.kt    # Protected resource access
│   ├── data/
│   │   └── QRCodeData.kt               # Data models
│   ├── security/
│   │   └── TokenManager.kt             # Secure token management
│   └── network/
│       ├── ApiService.kt               # Retrofit API interface
│       └── SecurityInterceptor.kt      # SSL pinning
├── res/
│   └── layout/
│       ├── activity_main.xml
│       ├── activity_scanner.xml
│       └── activity_resource_access.xml
└── AndroidManifest.xml
```

## Setup Instructions

### Prerequisites
- Android Studio 2021.1+
- Android SDK 24+
- Gradle 7.0+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/rickcoyle9-star/android-qr-auth-scanner.git
cd android-qr-auth-scanner
```

2. Configure your backend URL in `ApiService.kt`:
```kotlin
private const val BASE_URL = "https://your-backend-api.com/"
```

3. Build and run:
```bash
./gradlew assembleDebug
```

## Usage

### 1. Main Activity
- Launch the app
- Tap "Scan QR Code" to start scanning

### 2. Scanner Activity
- Point camera at QR code
- System automatically detects and validates
- On success, navigates to Resource Access

### 3. Resource Access Activity
- View authentication details
- Request access to protected resources
- Refresh token if needed

## QR Code Format

QR codes should contain JSON with this structure:

```json
{
  "user_id": "user123",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "resource_id": "resource_abc",
  "expiry": 1700000000000,
  "permissions": ["read", "write"],
  "nonce": "unique_identifier_12345"
}
```

## Security Features

### Encrypted Token Storage
Tokens are encrypted using Android KeyStore with AES-256-GCM:
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

### JWT Validation
- Verifies token signature
- Checks expiration time
- Validates issued-at (iat) claim

### Replay Attack Prevention
Nonce-based validation prevents token reuse:
```kotlin
fun isNonceValid(nonce: String): Boolean {
    if (nonce in cachedNonces) return false
    cachedNonces.add(nonce)
    return true
}
```

### SSL Certificate Pinning
Secure backend communication with pinned certificates:
```kotlin
val sslContext = SecurityInterceptor.createPinnedSSLContext(certificatePem)
```

## API Endpoints

The app expects these backend endpoints:

### POST `/api/v1/auth/validate-token`
Validate QR code token

**Request:**
```json
{
  "user_id": "user123",
  "token": "token_value",
  "resource_id": "resource_abc",
  "expiry": 1700000000000,
  "nonce": "unique_id"
}
```

**Response:**
```json
{
  "valid": true,
  "message": "Token is valid",
  "access_token": "new_access_token"
}
```

### GET `/api/v1/resources/{resourceId}`
Access protected resource

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "document_name": "Secret File",
    "content": "Protected content here"
  }
}
```

### POST `/api/v1/auth/refresh`
Refresh expired token

**Headers:**
```
Authorization: Bearer <refresh_token>
```

### POST `/api/v1/auth/log-event`
Log authentication events

**Request:**
```json
{
  "user_id": "user123",
  "resource_id": "resource_abc",
  "device_id": "device_id",
  "success": true,
  "timestamp": 1700000000000
}
```

## Permissions Required

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Dependencies

- **ML Kit Vision** - QR code detection
- **CameraX** - Camera integration
- **Retrofit** - HTTP client
- **OkHttp** - Network security
- **Android Security Crypto** - Encrypted SharedPreferences
- **JWT** - Token validation
- **Gson** - JSON parsing

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| Camera permission denied | User rejected permission | Grant camera permission in settings |
| Invalid or expired QR code | Token outside validity window | Generate new QR code |
| Potential replay attack detected | Nonce already used | Use fresh QR code |
| Token refresh failed | Invalid refresh token | Re-authenticate |
| Access denied | Insufficient permissions | Check user permissions |

## Testing

### Generate Test QR Code

```python
import qrcode
import json

data = {
    "user_id": "test_user",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "resource_id": "resource_123",
    "expiry": 9999999999000,
    "permissions": ["read", "write"],
    "nonce": "test_nonce_12345"
}

qr = qrcode.QRCode()
qr.add_data(json.dumps(data))
qr.make()
qr.make_image().save("test_qr.png")
```

## Backend Implementation Example

See [backend documentation](./BACKEND.md) for Node.js/Express example server.

## Security Best Practices

1. ✅ Always use HTTPS for backend communication
2. ✅ Implement token expiration (15-30 minutes recommended)
3. ✅ Use unique nonces for each QR code
4. ✅ Enable ProGuard/R8 in release builds
5. ✅ Rotate refresh tokens regularly
6. ✅ Validate JWTs on backend
7. ✅ Log all authentication attempts
8. ✅ Implement rate limiting

## Troubleshooting

### QR Code Not Scanning
- Ensure sufficient lighting
- Keep device steady
- Check camera permissions
- Verify QR code format is valid JSON

### Network Errors
- Check backend URL configuration
- Verify HTTPS certificate validity
- Test network connectivity
- Check firewall/proxy settings

### Token Validation Fails
- Verify token expiration time
- Check nonce hasn't been used before
- Validate JWT signature on backend
- Check token format and structure

## Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

MIT License - See LICENSE file for details

## Support

For issues or questions:
- Open GitHub Issues
- Check existing documentation
- Review security best practices

---

**⚠️ Security Notice**: This is a sample implementation. For production use:
- Implement comprehensive security audits
- Use proven authentication libraries
- Implement additional security measures
- Follow OWASP Mobile Security guidelines
