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

### Authentication & Token Management

The app uses a *JWT access/refresh token* pair stored in localStorage. The authService.js singleton manages the full lifecycle:

| Token | Lifetime | Storage | Purpose |
|-------|----------|---------|---------|
| Access token | ~15 minutes | localStorage | Authenticates API requests via Authorization: Bearer header |
| Refresh token | Longer-lived | localStorage | Obtains new access tokens without re-login |

*Token refresh* happens proactively — a timer fires 60 seconds before the access token expires, calling /api/auth/refresh. If the refresh succeeds, the new tokens replace the old ones silently. The user never sees this.

*When refresh fails* (expired refresh token, network error, server rejects):
- All auth data is cleared from localStorage
- isAuthenticated is set to false
- The user is redirected to the login page
- Any cached profile/contact data is purged (userService.clearCache())

*On app startup*, authService.initialize() checks localStorage for existing tokens. If an access token is expired, it attempts a refresh immediately. If that fails, the user starts unauthenticated and sees the login page.

---

### Route Protection (PrivateRoute.jsx)

All authenticated routes are wrapped in a PrivateRoute component:
```text
User navigates to /chat/:id
        │
        ▼
┌─────────────────────────┐
│ Is attemptedAuth true?  │── No ──► Show loading spinner
│ (auth initialized yet?) │
└──────────┬──────────────┘
           │ Yes
           ▼
┌─────────────────────────┐
│ Is isAuthenticated true?│── No ──► Redirect to /login
└──────────┬──────────────┘
           │ Yes
           ▼
     Render the page

This prevents unauthenticated users from accessing any page behind the MainLayout. The attemptedAuth flag prevents a flash of the login page while tokens are being validated on initial load.
```
---

### API Request Authentication

Every API call goes through service classes that attach the JWT via a shared helper:

const getHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

If a request returns *401 Unauthorized*, the user's auth state is cleared and they're redirected to login. There's no automatic retry — the refresh timer handles keeping the token fresh proactively.

---

### Server-Side Authorization (Backend Enforced)

The frontend sends the JWT with every request, but *the backend is the authority* on whether a user can access a resource. Examples:

- *Accessing a conversation you're not a member of:* The backend returns 403 Forbidden. The frontend displays an error message.
- *Editing/deleting someone else's message:* The backend rejects it. The frontend only shows edit/delete buttons on your own messages (isOwn flag).
- *Group admin actions (kick, promote, rename):* The UI only shows these controls to users with the ADMIN role, but the backend validates again on every request.

The frontend uses UI-level hiding as a convenience, not as security.

---

### WebSocket Authentication

The STOMP WebSocket connection authenticates once on connect by passing the JWT in the Authorization header:

connectHeaders: {
  Authorization: `Bearer ${token}`
}

The chat service backend validates this token on CONNECT. If the token expires while connected, the socket disconnects with an error, and the STOMP client's auto-reconnect fires — which uses a fresh token from localStorage (refreshed by the timer). If the refresh also failed and the user was logged out, the reconnection attempt sends no token and is rejected.

*Message-level authorization:* Even after connection, the backend checks that the user is a participant of the conversation before routing messages. For direct messages, it verifies the sender/recipient match. For group messages, it checks group membership.

---

### Push Notification Subscription

Push notifications use the *Web Push API* with VAPID keys:

1. Frontend fetches the VAPID public key from GET /api/push/vapid-public-key (requires JWT)
2. Browser creates a push subscription with FCM (Chrome) or equivalent
3. Subscription details are sent to POST /api/push/subscribe (requires JWT)
4. Backend stores the subscription keyed to the authenticated user

The subscription is tied to both the browser and the VAPID key pair. If VAPID keys change, old subscriptions become invalid (FCM returns 403) and the frontend re-subscribes automatically.

---

### Logout & Cleanup

On logout:
1. POST /api/auth/logout is called (best-effort — errors are swallowed)
2. localStorage is cleared (tokens, user info, expiry)
3. Auth state is reset
4. Profile and contact caches are purged
5. WebSocket is disconnected
6. User is redirected to /login

---

### Security Considerations

| Area | Approach |
|------|----------|
| *Token storage* | localStorage (acceptable for this app; HttpOnly cookies would be stronger but add complexity) |
| *Token lifetime* | Short access tokens (15 min) with silent refresh |
| *XSS* | React's default escaping prevents injection; no dangerouslySetInnerHTML except in search result highlighting (server-provided HTML) |
| *CSRF* | JWTs in Authorization header are not automatically sent by browsers, so CSRF is not a concern |
| *Dev vs Prod logging* | All console.log`/console.error` calls are wrapped with import.meta.env.DEV checks — stripped from production builds |
| *Error messages* | Generic messages shown to users ("Login failed. Please try again."); detailed errors only logged in dev |
### Input Validation

- *Client-side validation*: Immediate feedback using react-hook-form
- *Server validation*: Backend validates all inputs
- *Sanitization*: Prevent XSS with proper escaping
- *Type safety*: TypeScript prevents type-related vulnerabilities

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
