# Feature Status Overview

> **Current Date:** 2026-03-16  
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
  - Shared `JwtService` across all services for token validation

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
  - Automatically managed by WebSocket connect/disconnect events

### 👥 Contact Management (User Service — Port 8082)
- **Add Contact** (`POST /api/users/{userId}/contacts/{contactUserId}`)
  - Validates: cannot add yourself, prevents duplicates
- **List Contacts** (`GET /api/users/{userId}/contacts`)
  - Returns contact info enriched with profile data (displayName, avatarUrl, online status)
- **Remove Contact** (`DELETE /api/users/{userId}/contacts/{contactUserId}`)

### 💬 Conversations (Message Service — Port 8084)
- **List Conversations** (`GET /api/conversations`)
  - Returns all conversations for the authenticated user
- **Get or Create Direct Conversation** (`POST /api/conversations/direct/{targetUserId}`)
  - Idempotent: returns existing conversation if already present
- **Create Group Conversation** (`POST /api/conversations/group`)
  - Requires minimum 3 participants (including creator)
  - Supports optional group name

### 📨 Message History (Message Service — Port 8084)
- **Get Messages** (`GET /api/messages/{conversationId}?page=0&size=50`)
  - Paginated, reverse-chronological order (newest first)
  - Validates that the requesting user is a conversation participant

### ⚡ Real-time Messaging (Chat Service — Port 8083)
- **WebSocket/STOMP endpoint** (`/ws`)
  - JWT authentication for WebSocket connections
  - `@MessageMapping("/chat.send")` — send messages
  - Routes `GROUP` messages to `/topic/group/{conversationId}`
  - Routes `DIRECT` messages to `/queue/messages` (private queue)
  - `TYPING` indicator messages handled in-memory (not persisted)
- **Async message persistence via RabbitMQ**
  - Chat messages published to `chat.exchange` on send
  - `ChatMessageListener` (Message Service) consumes events and persists to DB
  - Conversation auto-created if it doesn't exist yet
- **Online/Offline status via WebSocket lifecycle**
  - On connect: sets `user:{userId}:online = true` in Redis
  - On disconnect: removes Redis key

### 🌐 API Gateway (Port 8080)
- Single entry point routing to all backend services
- Routes: `/api/auth/**`, `/api/users/**`, `/api/messages/**`, `/api/conversations/**`, `/ws/**`
- Swagger UI aggregation at `/swagger-ui.html`

### 🏗️ Infrastructure & Cross-cutting Concerns
- **Per-service PostgreSQL databases** (auth_db, user_db, message_db) for data isolation
- **Redis** for online status caching
- **RabbitMQ** for async inter-service events (`UserRegisteredEvent`, `ChatMessageEvent`)
- **Transaction-safe event publishing** — RabbitMQ messages sent only after DB transaction commits
- **Centralized exception handling** with structured `ErrorResponse` format
- **SpringDoc OpenAPI (Swagger)** documentation per service
- **Docker Compose** for full infrastructure stack

---

## ⚠️ Partially Implemented Features

### Contact Request Workflow
- **Status:** Entity supports `PENDING`, `ACCEPTED`, and `BLOCKED` states, but the API does not expose them
- **Missing:** Accept/reject contact request endpoints; block/unblock endpoint
- **Current behaviour:** All added contacts are persisted without a status transition flow

### Typing Indicators
- **Status:** `TYPING` message type is handled and routed via WebSocket
- **Missing:** No debouncing, room-level broadcast, or client-side timeout management defined server-side

### MinIO Object Storage
- **Status:** MinIO is declared in `docker-compose.yml` but not wired into any service
- **Missing:** Profile picture / file attachment upload endpoints and MinIO client integration

---

## ❌ Not Yet Implemented Features

| Feature | Notes |
|---|---|
| File attachments in messages | `Message.content` is text-only (max 2,000 chars); no blob/URL field |
| Read receipts / delivery status | No `readAt` or `deliveredAt` tracking on messages |
| Message search / filtering | No full-text search on message content |
| Account deletion / deactivation | No soft-delete or closure endpoint |
| Push notifications | No FCM/APNs or webhook integration |
| Pagination on contact list | Contact list endpoint returns all contacts with no paging |
| Contact-Profile JPA relation | Currently a UUID reference with manual lookup (see `technical-debt.md`) |
| Remove `displayName` from Auth Service | Auth Service still carries a `displayName` that becomes stale after profile updates (see `technical-debt.md`) |

---

## 🕐 Features Implemented in the Last 6 Days

> All development activity between **2026-03-10** and **2026-03-16** is listed below.

### Commit `504421e` — *2026-03-13* — "Make convertAndSend use transaction"

This commit introduced the **entire project** from scratch, including:

#### Auth Service
- User registration endpoint with JWT issuance
- User login endpoint with JWT issuance
- `AuthService` with `TransactionSynchronizationManager` — `UserRegisteredEvent` is now published to RabbitMQ **only after** the DB transaction commits, preventing ghost events on rollback

#### User Service
- Profile CRUD endpoints (create, read, update)
- Profile search by display name
- Online status endpoint backed by Redis
- Contact management endpoints (add, list, remove)
- `UserEventListener` — idempotent profile creation on `UserRegisteredEvent`

#### Chat Service
- WebSocket/STOMP server with JWT authentication
- `ChatController` handling `CHAT`, `GROUP`, and `TYPING` message types
- `WebSocketEventListener` managing Redis online status on connect/disconnect

#### Message Service
- Conversation endpoints (list, get/create direct, create group)
- Message history endpoint (paginated)
- `ChatMessageListener` — persists incoming `ChatMessageEvent`s and auto-creates conversations

#### API Gateway
- Routing configuration for all services
- Swagger UI aggregation

#### Shared Library
- Common exceptions (`BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `ResourceNotFoundException`)
- `ApiResponse` / `ErrorResponse` DTOs
- `RabbitMQConstants`
- `UserRegisteredEvent` DTO

#### Infrastructure
- Docker Compose stack (PostgreSQL ×3, Redis, RabbitMQ, MinIO)
- Per-service Dockerfiles and Maven build configuration
