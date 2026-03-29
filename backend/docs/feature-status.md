# Feature Status Overview
> **Current Date:** 2026-03-29
> **Project:** Orang Chat — Cloud-native microservices chat application

---

## ✅ Fully Implemented Features

### 🔐 Authentication (Auth Service — Port 8081)
- **User Registration** (`POST /api/auth/register`)
  - Email, password, and display name
  - Returns JWT access token, refresh token, userId, tokenType, expiresIn
  - Publishes `UserRegisteredEvent` to RabbitMQ to trigger async profile creation
- **User Login** (`POST /api/auth/login`)
  - Email/password authentication with BCrypt-encoded password verification
  - Returns same JWT response as registration
- **Token Refresh** (`POST /api/auth/refresh`)
  - Refresh token rotation and reuse detection implemented
- **JWT Issuance & Validation**
  - HS256-signed tokens with configurable secret and expiry
  - Shared JWT utilities used by downstream services for validation/parsing

### 👤 User Profiles (User Service — Port 8082)
- **Profile CRUD**
  - Get, Create, Update profile (`displayName`, `avatarUrl`, `bio`)
  - Automatic idempotent creation via `UserRegisteredEvent`
- **Search Profiles** (`GET /api/users/search?query=`)
  - Search users by display name
- **Online Status**
  - Stored in Redis for fast access
  - Managed automatically by WebSocket connect/disconnect events
  - Manual update via `POST /api/users/{userId}/online`

### 👥 Contact Management (User Service — Port 8082)
- **Contact Request Workflow**
  - Send request (`POST /api/contacts/request/{targetUserId}`)
  - Accept request (`POST /api/contacts/{contactId}/accept`)
  - Reject/Cancel/Remove contact
- **Blocking System**
  - Block user (`POST /api/contacts/block/{targetUserId}`)
  - List blocked users (`GET /api/contacts/blocked`)
- **Contact Lists**
  - List accepted contacts, incoming requests, and outgoing requests

### 💬 Conversations & Messaging (Message Service — Port 8084)
- **Conversation Management**
  - List user conversations, open direct or group conversations
- **Message History** (`GET /api/messages/{conversationId}`)
  - Paginated retrieval of messages with participant authorization
- **Async Message Persistence**
  - Consumes messages from RabbitMQ for durable storage
- **Read Receipts Persistence**
  - Consumes and persists read receipts for messages

### ⚡ Real-time Messaging (Chat Service — Port 8083)
- **WebSocket/STOMP endpoint** (`/ws`)
  - JWT authentication for WebSocket connections
  - Direct and Group message relay
  - Typing indicators (in-memory)
  - Real-time Read Receipt broadcasting
- **Presence Lifecycle**
  - Automatic online/offline status updates on WebSocket connection events

### 🌐 API Gateway (Port 8080)
- Single entry point routing for all backend services
- Routes: `/api/auth/**`, `/api/contacts/**`, `/api/users/**`, `/api/messages/**`, `/api/conversations/**`, `/ws/**`
- Swagger UI aggregation at `/swagger-ui.html`

---

## 🟡 In Progress / Planned
- [ ] **Group Chat Persistence**: Durable storage for group-specific messages
- [ ] **Versioned Migrations**: Replacing Hibernate `ddl-auto` with Flyway/Liquibase (Partially started with V2 migration in user-service)
- [ ] **Media Storage**: MinIO integration for profile pictures and attachments
- [ ] **Automated Testing**: Increasing coverage for controllers and cross-service integrations
- [ ] **CI/CD Pipeline**: Automated build, test, and deployment
- [ ] **Observability**: Centralized logging, metrics (Prometheus/Grafana), and tracing (Jaeger)
