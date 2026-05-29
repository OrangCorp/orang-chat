# Authentication & Authorization

## Backend
- **JWT-based Authentication**: All backend services (auth, user, chat, notification, message) use JWT for stateless authentication. JWT secrets are injected via environment variables (`JWT_SECRET`).
- **Spring Security**: Services use `spring-boot-starter-security` (see each service's `pom.xml`) for authentication and authorization enforcement.
- **Password Handling**: Passwords for databases, Redis, RabbitMQ, and mail are injected via environment variables and never hardcoded.
- **Password Reset**: Auth service supports password reset tokens with expiry (`password-reset.token-expiry-hours`).
- **Role-based Access**: Role and permission enforcement is handled via Spring Security (see `pom.xml` and likely code in each service).

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
