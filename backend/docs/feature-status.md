# Feature Status Overview

Current Date: 2026-04-02
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
- Remove contact endpoint exists: DELETE /api/contacts/{contactId}
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
- History: GET /api/messages/{conversationId}
- Search: GET /api/messages/{conversationId}/search
- Context around message: GET /api/messages/{conversationId}/around/{messageId}
- Async persistence from RabbitMQ route chat.message.sent

### 1.4 Chat Service (port 8083)

Realtime:
- STOMP endpoint: /ws
- Inbound mapping: /app/chat.send
- Routes direct messages to user queue and group messages to topic
- Typing indicator routing for MessageType.TYPING (broadcast only)

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

### 2.1 Contact removal has a functional bug

Current state:
- removeContact validates input and authorization but does not delete the contact entity.

Impact:
- Endpoint can return success without actually removing contact relationship.

### 2.2 WebSocket auth error handling is too generic

Current state:
- RuntimeException is thrown for invalid/missing JWT header in STOMP CONNECT interceptor.

Impact:
- Poor error semantics and weaker operational diagnostics.

### 2.3 JWT filters swallow errors silently

Current state:
- User service and message service JWT filters catch Exception and ignore it.

Impact:
- Harder troubleshooting and weaker security observability.

### 2.4 WebSocket CORS is permissive

Current state:
- setAllowedOriginPatterns("*")

Impact:
- Too open for production unless constrained by external network controls.

---

## 3. Not Implemented Yet

Messaging domain gaps:
- Message edit
- Message delete
- Message reactions
- Message pinning
- Read receipts (payload type, persistence flow, API retrieval)
- Attachments/media metadata and storage

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
- Fix ContactService.removeContact to perform delete.
- Replace silent auth filter catches with logging + explicit auth handling.
- Replace generic RuntimeException in WebSocket security path.

P1 (next sprint):
- Implement read receipts end-to-end.
- Add edit/delete message APIs and service logic.
- Add tests for contact/conversation/message services.

P2 (after core reliability):
- Add attachments/media pipeline.
- Add observability stack and SLO metrics.
- Complete migration strategy standardization.
