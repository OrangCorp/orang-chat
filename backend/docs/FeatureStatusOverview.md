# Feature Status Overview

> **Current Date:** 2026-03-28
> **Project:** Orang Chat — Cloud-native microservices chat application

---

## ✅ Fully Implemented Features

### 🔐 Authentication (Auth Service — Port 8081)
- **User Registration** (`POST /api/auth/register`)
  - Email, password, and display name
  - Returns JWT access token, refresh token, userId, tokenType, expiresIn
  - Publishes `UserRegisteredEvent` to RabbitMQ (after transaction commit) to trigger async profile creation
- **User Login** (`POST /api/auth/login`)
  - Email/password authentication with BCrypt-encoded password verification
  - Returns same JWT response as registration (including refresh token)
- **Token Refresh** (`POST /api/auth/refresh`)
  - Exchange valid refresh token for new access and refresh tokens
  - Implements rotation and reuse detection to prevent token theft
- **JWT Issuance & Validation**
  - HS256-signed tokens with configurable secret and expiry
  - Support for access and refresh token lifetimes
  - Token rotation and reuse detection implemented
  - Shared JWT utilities are used by downstream services for validation/parsing

### 👤 User Profiles (User Service — Port 8082)
- **Get Profile** (`GET /api/users/{userId}/profile`)
- **Create Profile** (`POST /api/users/{userId}/profile`)
  - Also triggered automatically via `UserRegisteredEvent` from RabbitMQ (idempotent)
- **Update Profile** (`PUT /api/users/{userId}/profile`)
  - Supports updating `displayName`, `avatarUrl`, and `bio`
- **Search Profiles** (`GET /api/users/search?query=`)
  - Search users by display name
- **Online Status** (`POST /api/users/{userId}/online?status=true|false`)
  - Stored in Redis for fast access
  - Can be updated explicitly via REST
  - Also managed automatically by WebSocket connect/disconnect events

### 👥 Contact Management (User Service — Port 8082)
- **Add Contact** (`POST /api/users/{userId}/contacts/{contactUserId}`)
  - Validates: cannot add yourself, prevents duplicates
- **List Contacts** (`GET /api/users/{userId}/contacts`)
  - Returns contact info enriched with profile data (displayName, avatarUrl, online status)
- **Remove Contact** (`DELETE /api/users/{userId}/contacts/{contactUserId}`)

### ⚡ Real-time Messaging (Chat Service — Port 8083)
- **WebSocket/STOMP endpoint** (`/ws`)
  - JWT authentication for WebSocket connections
  - `@MessageMapping("/chat.send")` — send messages
  - Routes `GROUP` messages to `/topic/group/{conversationId}`
  - Routes `DIRECT` messages to `/queue/messages` (private queue)
  - `TYPING` indicator messages handled in-memory
  - `@MessageMapping("/chat.receipt")` — handle and broadcast message read receipts
- **Online/Offline status via WebSocket lifecycle**
  - On connect: sets `user:{userId}:online = true` in Redis
  - On disconnect: removes Redis key
  - Login itself does **not** mark the user online

### 📝 Persistence & History (Message Service — Port 8084)
- **Conversation Management**
  - List user conversations, open direct or group conversations
- **Message History** (`GET /api/messages/{conversationId}`)
  - Paginated retrieval of messages with membership authorization
- **Async Message Persistence**
  - Consumes messages from RabbitMQ for durable storage
- **Read Receipts Persistence**
  - Consumes and persists read receipts for messages (tracking who read what and when)

### 🌐 API Gateway (Port 8080)
- Single entry point routing to all backend services
- Routes: `/api/auth/**`, `/api/users/**`, `/api/messages/**`, `/api/conversations/**`, `/ws/**`
- Swagger UI aggregation at `/swagger-ui.html`
