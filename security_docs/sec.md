## Backend

### 1. Authentication
- **JWT with HMAC-SHA256**: Stateless, microservices-friendly
- **Password Hashing**: BCrypt (Spring Security default)
- **Token Reuse Detection**: Redis markers + full session blacklist

### 2. Authorization
- **Role-Based**: Participant in conversation, contact status, group admin role
- **Fine-Grained**: Each service checks user eligibility for operation
- **API Annotations**: `@AuthenticationPrincipal` injects current userId

### 3. Encryption
- **Passwords**: BCrypt hashing (non-reversible)
- **JWT**: HMAC-SHA256 signing (verifies server created token)
- **Web Push**: ECDP256 asymmetric encryption (browser validates server)
- **Transport**: HTTPS recommended for production

### 4. Input Validation
- **DTOs**: All endpoints validate request bodies (null checks, length limits)
- **Message Content**: 2000 character max
- **Email**: RFC 5322 validation
- **Rate Limiting**: Prevents brute force attacks

### 5. Session Management
- **Access Token**: 15 minutes (short-lived, minimal damage if stolen)
- **Refresh Token**: 7 days (tied to device, separate expiry)
- **Logout**: Blacklist in Redis (can't reuse tokens)
- **Concurrent Sessions**: Tracked per user (can terminate specific sessions)

## Frontend
- **No secrets in code**: No authentication secrets are present in the frontend codebase.

# Docker & Deployment Security

- **Multi-stage Docker Builds**: Used for backend services to reduce image size and attack surface.
- **No secrets in images**: All secrets are injected at runtime, not baked into images.
- **Environment Variables**: Used for all sensitive config.
- **SSL Termination**: Handled by Nginx, not app containers.
- **Cloud Readiness**: Uses Spring Cloud and Docker Compose for scalable, cloud-native deployment.


# CORS & API Gateway Protections

- **CORS Enforcement**: Allowed origins are set via environment variable (`CORS_ALLOWED_ORIGINS`) and enforced in the API Gateway and Docker Compose.
- **Spring Cloud Gateway**: Used for routing and CORS enforcement (see api-gateway `pom.xml` and `application.yaml`).
- **No open CORS**: CORS is not set to `*`, reducing risk of cross-origin attacks.


- **All sensitive values** (database passwords, JWT secrets, Redis, RabbitMQ, mail, MinIO keys, VAPID keys) are injected via environment variables, not hardcoded in code or config.
- **Docker Compose** enforces required secrets with `${VAR:?error}` syntax, ensuring containers do not start without secrets.
- **No secrets in VCS**: No secrets are checked into version control.


# Logging & Monitoring

- **Configurable Logging Levels**: Logging levels for application, messaging, websocket, and security are set via environment variables.
- **Sensitive Data**: No evidence of logging sensitive data (passwords, secrets) in configs.
- **Spring Security Logging**: Security-related logs are enabled for audit and monitoring.
# Transport Security (HTTPS, TLS, Certificates)

- **Nginx SSL Termination**: Nginx is configured to terminate SSL/TLS on port 443, using certificates (`cert.pem`, `key.pem`).
- **HTTP to HTTPS Redirect**: All HTTP traffic is redirected to HTTPS (see nginx.conf).
- **Backend Services**: Rely on Nginx for HTTPS; internal traffic is not encrypted by default (typical for microservices behind a secure gateway).
