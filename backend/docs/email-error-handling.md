# Email Service Error Handling Documentation

## Overview

Custom error handling has been implemented for MailTrap SMTP connection failures in the auth service registration and login flows. This handles the specific error:

```
org.springframework.mail.MailSendException: Mail server connection failed. 
Failed messages: jakarta.mail.MessagingException: Could not convert socket to TLS;
nested exception is: java.net.SocketTimeoutException: Read timed out
```

## Architecture

### Components

#### 1. **EmailServiceException** (`shared-library`)
- **Location:** `backend/shared-library/src/main/java/com/orang/shared/exception/EmailServiceException.java`
- **Extends:** `BaseException`
- **HTTP Status:** `503 Service Unavailable`
- **Enum:** `EmailErrorType` categorizes different mail failures:
  - `CONNECTION_FAILED` - Mail server connection failed
  - `AUTHENTICATION_FAILED` - Mail server authentication failed
  - `TIMEOUT` - Mail server request timed out (includes SocketTimeoutException)
  - `TEMPLATE_ERROR` - Email template processing failed
  - `INVALID_EMAIL` - Invalid email address
  - `UNKNOWN` - Unspecified mail error

#### 2. **Enhanced EmailService** (`auth-service`)
- **Location:** `backend/auth-service/src/main/java/com/orang/authservice/service/EmailService.java`
- **Features:**
  - Catches `MailSendException` (Spring Mail wrapper)
  - Catches `MessagingException` (Jakarta Mail)
  - Catches `TemplateEngineException` (Thymeleaf)
  - Analyzes root causes to determine specific error type
  - Provides detailed logging with error context

#### 3. **Updated AuthService** (`auth-service`)
- **Location:** `backend/auth-service/src/main/java/com/orang/authservice/service/AuthService.java`
- **Enhanced Methods:**
  - `register()` - Sends verification email with error handling
  - `resendVerification()` - Resends verification code with error handling
  - `requestPasswordReset()` - Sends password reset email with error handling
- **Behavior:**
  - User data is saved even if email fails (non-blocking)
  - User receives clear error message with retry instructions
  - Error details logged for debugging

#### 4. **GlobalExceptionHandler** (`shared-library`)
- **Location:** `backend/shared-library/src/main/java/com/orang/shared/exception/GlobalExceptionHandler.java`
- **Handler:** New `handleEmailServiceException()` method
- **Returns:** HTTP 503 Service Unavailable with error details

## Error Handling Flow

### MailTrap Connection Timeout Scenario

```
┌─────────────────────────────────────────────────────────────┐
│ 1. EmailService.sendEmail() called                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. MailSendException caught                                 │
│    - Root cause: SocketTimeoutException (Read timed out)    │
│    - Nested: MessagingException: Could not convert to TLS   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. handleMailSendException() analyzes root cause            │
│    - Detects SocketTimeoutException                         │
│    - Maps to EmailErrorType.TIMEOUT                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Throw EmailServiceException with clear message:          │
│    "Email service is temporarily unavailable due to mail    │
│     server timeout. Please try again later."                │
│    - Status: 503                                            │
│    - ErrorType: TIMEOUT                                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. AuthService catches and re-throws with context:          │
│    - Logs specific error for debugging                      │
│    - Appends user instructions                              │
│    - Maintains user data for retry                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. GlobalExceptionHandler.handleEmailServiceException()     │
│    - Converts to ErrorResponse                              │
│    - Returns 503 HTTP response to client                    │
└─────────────────────────────────────────────────────────────┘
```

## Error Detection Strategy

### SocketTimeoutException Detection
```java
if (e.contains(SocketTimeoutException.class)) {
    // Timeout during socket read from MailTrap
    // Common cause: Network latency, MailTrap overload
}
```

### TLS Handshake Failure Detection
```java
if (rootCauseMsg != null && rootCauseMsg.contains("Could not convert socket to TLS")) {
    // TLS negotiation failed
    // Causes: Certificate issues, protocol mismatch, configuration error
}
```

### Authentication Failure Detection
```java
if (rootCauseMsg != null && rootCauseMsg.toLowerCase().contains("authentication")) {
    // SMTP authentication failed
    // Causes: Invalid credentials, account locked, permission issues
}
```

## Usage Examples

### Registration with Email Failure

**Request:**
```bash
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe"
}
```

**Response (Email Service Down):**
```json
{
  "timestamp": "2026-05-04T10:30:45.123456",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Account created successfully, but we couldn't send the verification email. Please try requesting a new verification email or check your spam folder.",
  "path": "/api/auth/register"
}
```

### Resend Verification with Timeout

**Request:**
```bash
POST /api/auth/resend-verification
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response (Mail Server Timeout):**
```json
{
  "timestamp": "2026-05-04T10:35:22.654321",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Unable to send verification email at this moment. Please try again in a few moments.",
  "path": "/api/auth/resend-verification"
}
```

## Logging

### Log Levels by Scenario

**INFO Level (Success):**
```
2026-05-04 10:30:45 INFO  - Email sent successfully to user@example.com: Verify your email — Orang Chat
```

**WARN Level (Email Failures):**
```
2026-05-04 10:31:00 WARN  - Mail send exception occurred for email to user@example.com: Socket connect timeout
2026-05-04 10:31:00 WARN  - Failed to resend verification email for user: 550e8400-e29b-41d4-a716-446655440000. Error: Email service is temporarily unavailable due to mail server timeout. (Type: TIMEOUT)
```

**ERROR Level (Critical Failures):**
```
2026-05-04 10:32:15 ERROR - Mail server connection timed out. MailTrap is not responding. Recipient: user@example.com
2026-05-04 10:32:15 ERROR - TLS handshake failed with MailTrap. Possible certificate or configuration issue. Recipient: user@example.com
2026-05-04 10:32:15 ERROR - MailTrap authentication failed. Check credentials. Error: Invalid credentials
```

## Configuration

### MailTrap Settings (`application.yaml`)

```yaml
spring:
  mail:
    host: sandbox.smtp.mailtrap.io
    port: 2525
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          writetimeout: 5000
```

**Key Timeout Settings:**
- `connectiontimeout: 5000` - 5 seconds for initial connection
- `writetimeout: 5000` - 5 seconds for write operations
- These can be increased if MailTrap is consistently slow

## Testing Error Scenarios

### Test 1: MailTrap Connection Timeout
```bash
# Temporarily block MailTrap connection
# Or reduce timeout values in application.yaml

curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "displayName": "Test User"
  }'

# Expected: 503 response with timeout message
```

### Test 2: Invalid MailTrap Credentials
```bash
# Update MAIL_USERNAME or MAIL_PASSWORD to invalid values
# Restart auth service

curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "displayName": "Test User"
  }'

# Expected: 503 response with authentication error message
```

### Test 3: Resend Verification After Email Failure
```bash
# Assuming user already created but email failed

curl -X POST http://localhost:8080/api/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'

# Expected: 503 if email service still down, user can retry
```

## Troubleshooting

### Issue: Always Getting Timeout Errors

**Causes:**
1. MailTrap instance down or unreachable
2. Network connectivity issues to MailTrap (firewall/proxy)
3. Timeout values too short (5 seconds)

**Solutions:**
```bash
# Test connectivity
nc -zv sandbox.smtp.mailtrap.io 2525

# Check network from pod (if containerized)
kubectl exec -it <pod> -- nc -zv sandbox.smtp.mailtrap.io 2525

# Increase timeout values in application.yaml
connectiontimeout: 10000  # 10 seconds
writetimeout: 10000       # 10 seconds
```

### Issue: Authentication Always Fails

**Causes:**
1. Invalid credentials in environment variables
2. MailTrap account credentials changed
3. Account suspended/locked

**Solutions:**
```bash
# Verify credentials
echo $MAIL_USERNAME
echo $MAIL_PASSWORD

# Test MailTrap credentials manually
telnet sandbox.smtp.mailtrap.io 2525

# Check MailTrap dashboard for account status
```

### Issue: TLS Handshake Failures

**Causes:**
1. MailTrap certificate issues
2. Java SSL/TLS configuration problems
3. JVM truststore missing MailTrap certificate

**Solutions:**
```bash
# Verify Java can reach MailTrap SSL
openssl s_client -connect sandbox.smtp.mailtrap.io:2525

# Check if STARTTLS is enabled in application.yaml
# Ensure mail.smtp.starttls.enable=true
```

## Monitoring

### Metrics to Track

1. **Email Send Success Rate**
   - Gauge: `email.send.success.total`
   - Alert: < 95% success rate

2. **Email Send Failure Rate by Type**
   - Gauge: `email.send.failure.timeout`
   - Gauge: `email.send.failure.auth`
   - Gauge: `email.send.failure.connection`

3. **Registration with Failed Email**
   - Counter: `auth.register.email_failure.total`
   - Alert: > 5 per minute

### Log Monitoring

Monitor for these patterns in application logs:
```
- "Mail server connection timed out"
- "TLS handshake failed"
- "MailTrap authentication failed"
- "Email service is temporarily unavailable"
```

## Future Improvements

1. **Retry Logic:** Implement exponential backoff for failed email sends
2. **Async Email:** Move email sending to async task queue
3. **Email Template Caching:** Cache compiled templates to reduce failures
4. **MailTrap Health Check:** Periodic endpoint to check MailTrap availability
5. **User Notifications:** Notify users when email failures are resolved
6. **Alternative Mail Provider:** Fallback to secondary mail service if MailTrap fails

## References

- [Spring Mail Documentation](https://docs.spring.io/spring-framework/reference/integration/mail.html)
- [Jakarta Mail API](https://eclipse-ee4j.github.io/mail/)
- [MailTrap Documentation](https://mailtrap.io/blog/send-emails-from-java-spring-boot/)
- [Spring Transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)
