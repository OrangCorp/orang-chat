# Feature Status Overview

Current Date: 2026-04-09
Project: Orang Chat backend microservices
Source of truth: backend code (controllers, services, listeners, config, tests)

---

## 1. Implemented Features

### 1.1 Auth Service (port 8081)

Implemented and wired:
- Register: POST /api/auth/register
- Login: POST /api/auth/login
- Logout (blacklist): POST /api/auth/logout
- Refresh with reuse detection: POST /api/auth/refresh
- UserRegisteredEvent publish to RabbitMQ after successful commit

Notes:
- Access and refresh tokens are generated with JWT.
- Refresh-token reuse detection is implemented with Redis token markers and user blacklist.

### 1.2 User Service (port 8082)

Profiles:
- Get profile: GET /api/users/{userId}/profile
- Create profile: POST /api/users/{userId}/profile
- Update profile: PUT /api/users/{userId}/profile
- Search profiles: GET /api/users/search?query=
- Batch summaries: POST /api/users/batch

Contacts:
- Send request: POST /api/contacts/request/{targetUserId}
- Accept request: POST /api/contacts/{contactId}/accept
- Reject request: POST /api/contacts/{contactId}/reject
- Cancel request: POST /api/contacts/{contactId}/cancel
- Remove contact: DELETE /api/contacts/{contactId}
- Block user: POST /api/contacts/block/{targetUserId}
- Unblock user: DELETE /api/contacts/block/{targetUserId}
- List accepted/incoming/outgoing/blocked

Presence:
- User status: GET /api/users/{userId}/status
- Batch status: POST /api/users/status/batch
- Last seen: GET /api/users/{userId}/last-seen
- Session list: GET /api/users/{userId}/sessions
- Session termination: DELETE /api/users/{userId}/sessions/{sessionId}

Event-driven profile bootstrap:
- UserRegisteredEvent listener creates profile idempotently.

### 1.3 Message Service (port 8084)

Conversations:
- List conversations: GET /api/conversations
- Create/get direct: POST /api/conversations/direct/{targetUserId}
- Create group: POST /api/conversations/group
- Add participants: POST /api/conversations/{conversationId}/participants
- Remove participant: DELETE /api/conversations/{conversationId}/participants/{userId}
- Leave: POST /api/conversations/{conversationId}/leave
- Rename: PUT /api/conversations/{conversationId}
- Promote: POST /api/conversations/{conversationId}/participants/{userId}/promote
- Demote: POST /api/conversations/{conversationId}/participants/{userId}/demote
- Delete group: DELETE /api/conversations/{conversationId}

Messages:
- Send message (REST): POST /api/messages (added for testing/fallback)
- History: GET /api/messages/{conversationId}
- Search: GET /api/messages/{conversationId}/search
- Context around message: GET /api/messages/{conversationId}/around/{messageId}
- Edit message: PUT /api/messages/{messageId}
- Delete message: DELETE /api/messages/{messageId}
- Async persistence from RabbitMQ route chat.message.sent

Reactions, Read Receipts & Notifications:
- Add/remove reaction: POST /api/reactions/{messageId}
- List reactions: GET /api/reactions/{messageId}
- Update read receipt: POST /api/read-receipts/{conversationId}
- Get read receipts: GET /api/read-receipts/{conversationId}
- Mute/Unmute notifications: POST /api/conversations/{id}/notifications/mute|unmute
- Get notification preferences: GET /api/conversations/{id}/notifications

Attachments & Thumbnails:
- Upload: POST /api/attachments/upload
- Download: GET /api/attachments/{attachmentId}/download
- Metadata: GET /api/attachments/{attachmentId}/metadata
- Thumbnail: GET /api/attachments/{attachmentId}/thumbnail
- STOMP support: `ChatMessagePayload` includes `attachmentIds`
- Async thumbnails: RabbitMQ-driven generation with WebSocket notification (`THUMBNAIL_READY`)
- Pin message: POST /api/pinned-messages/{messageId}
- Unpin message: DELETE /api/pinned-messages/{messageId}
- List pinned: GET /api/pinned-messages/{conversationId}

### 1.4 Chat Service (port 8083)

Realtime:
- STOMP endpoint: /ws
- Inbound mapping: /app/chat.send
- Routes direct messages to user queue and group messages to topic
- Typing indicator routing for MessageType.TYPING (broadcast only)
- Realtime thumbnail notifications: `THUMBNAIL_READY` event broadcast to group topics

Presence lifecycle:
- Session add on WebSocket connect
- Session removal on WebSocket disconnect
- Heartbeat endpoint: /app/presence.heartbeat

Group events:
- Rabbit listener consumes chat.group.events and broadcasts to group topics.

### 1.5 API Gateway (port 8080)

Implemented:
- Route forwarding for auth, user/contact, message/conversation, and websocket paths.
- Request rate limiting configured for auth/user/message routes.
- Swagger aggregation enabled on /swagger-ui.html.

---

## 2. Partially Implemented or Risky Areas

### 2.1 WebSocket auth error handling is too generic

Current state:
- RuntimeException is thrown for invalid/missing JWT header in STOMP CONNECT interceptor.

Impact:
- Poor error semantics and weaker operational diagnostics.

### 2.2 JWT filter swallows errors silently (user-service)

Current state:
- User service JWT filter catches Exception and ignores it. (Fixed in message-service).

Impact:
- Harder troubleshooting in user-service and weaker security observability.

### 2.3 WebSocket CORS is permissive

Current state:
- setAllowedOriginPatterns("*")

Impact:
- Too open for production unless constrained by external network controls.

---

## 3. Not Implemented Yet / Known Issues

Messaging domain gaps:
- WebSocket direct message routing logic (Needs verification for self-messaging edge cases).
- REST message sending endpoint is added to code but may require container restart to be active in some environments.

Platform/ops gaps:
- Flyway/Liquibase migration ownership across all services
- Comprehensive integration tests for async flows
- Production-grade observability (metrics/tracing/log correlation)
- CI quality gates for service-level tests

---

## 4. Test Coverage Snapshot

Observed tests:
- Stronger unit coverage in auth service (AuthService/JWT)
- Presence coverage in user service
- Mostly application-context tests in chat/message/gateway/shared modules

Missing tests with highest impact:
- ContactService workflow and edge cases
- ConversationService authorization/role transitions
- ChatMessageListener retry/failure behavior
- WebSocket controller and security flows

---

## 5. Implementation Priority

P0 (fix immediately):
- Replace silent auth filter catch in user-service with logging + explicit auth handling.
- Replace generic RuntimeException in WebSocket security path.
- Restrict WebSocket CORS by environment.

P1 (next sprint):
- Add tests for contact/conversation/message services.
- Expand service-level integration tests for async flows.

P2 (after core reliability):
- Add observability stack and SLO metrics.
- Complete migration strategy standardization.
