# Feature Status Overview

> **Current Date:** 2026-03-28
> **Project:** Orang Chat — Cloud-native microservices chat application

---

## ✅ Fully Implemented Features

### 🔐 Authentication (Auth Service — Port 8081)
- **User Registration** (`POST /api/auth/register`)
  - Email, password, and display name
  - Returns JWT access token, userId, tokenType, expiresIn
  - Publishes `UserRegisteredEvent` to RabbitMQ (after transaction commit) to trigger async profile creation
- **User Login** (`POST /api/auth/login`)
  - Email/password authentication with BCrypt-encoded password verification
  - Returns same JWT response as registration
- **JWT Issuance & Validation**
  - HS256-signed tokens with configurable secret and expiry
  - Token lifetime is currently configured via `jwt.expiration` in Auth Service
  - Shared JWT utilities are used by downstream services for validation/parsing
- **Token Refresh** (`POST /api/auth/refresh`)
  - Refresh token rotation and reuse detection implemented

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

### 💬 Conversations & Messaging (Message Service — Port 8084)
- **Conversation Management**
  - **List Conversations** (`GET /api/conversations`) — Returns list of conversations for the authenticated user
  - **Direct Chat** (`POST /api/conversations/direct/{targetUserId}`) — Gets or creates a direct conversation between two users
  - **Group Chat** (`POST /api/conversations/group`) — Creates a group conversation with multiple participants
- **Message History**
  - **Get History** (`GET /api/messages/{conversationId}`) — Paginated retrieval of messages with participant authorization
- **Async Persistence**
  - Consumes `chat.message.sent` from RabbitMQ to persist messages sent over WebSocket

### ⚡ Real-time Messaging (Chat Service — Port 8083)
- **WebSocket/STOMP endpoint** (`/ws`)
  - JWT authentication for WebSocket connections
  - `@MessageMapping("/chat.send")` — send messages (CHAT, JOIN, LEAVE, TYPING, GROUP)
  - Routes `GROUP` messages to `/topic/group/{conversationId}`
  - Routes `DIRECT` messages to `/queue/messages` (private queue)
  - `TYPING` indicator messages handled in-memory (not persisted)
- **Read Receipts**
  - `@MessageMapping("/chat.receipt")` — receives read receipts from clients
  - Broadcasts receipts to relevant users over WebSocket
  - Publishes `MessageReceiptEvent` to RabbitMQ for persistence in Message Service
- **Online/Offline status via WebSocket lifecycle**
  - On connect: sets `user:{userId}:online = true` in Redis
  - On disconnect: removes Redis key

### 🌐 API Gateway (Port 8080)
- Single entry point routing to all backend services
- Routes: `/api/auth/**`, `/api/users/**`, `/api/messages/**`, `/api/conversations/**`, `/ws/**`
- Swagger UI aggregation at `/swagger-ui.html`
