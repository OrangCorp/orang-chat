# Backend Implementation Status

Generated from source code, repository documentation, and recent commit history on branch `dev` as of 2026-03-13. Code is treated as the source of truth where documentation and implementation diverge.

## ✅ Completed

- Feature: Authentication and JWT issuance
  Description: User registration and login are implemented with email/password validation, password hashing, JWT generation, and structured auth responses. Registration also emits a user-created domain event for downstream services.
  Key Files: `auth-service/src/main/java/com/orang/authservice/controller/AuthController.java`, `auth-service/src/main/java/com/orang/authservice/service/AuthService.java`, `auth-service/src/main/java/com/orang/authservice/service/JwtService.java`, `auth-service/src/main/java/com/orang/authservice/entity/User.java`, `auth-service/src/main/java/com/orang/authservice/repository/UserRepository.java`
  Related Endpoints: `POST /api/auth/register`, `POST /api/auth/login`

- Feature: Event-driven profile bootstrap after registration
  Description: Auth service publishes `UserRegisteredEvent` via RabbitMQ, and user-service consumes it to create a profile idempotently. This is a working cross-service integration, not just a planned pattern.
  Key Files: `auth-service/src/main/java/com/orang/authservice/service/AuthService.java`, `shared-library/src/main/java/com/orang/shared/event/UserRegisteredEvent.java`, `user-service/src/main/java/com/orang/userservice/listener/UserEventListener.java`, `user-service/src/main/java/com/orang/userservice/service/ProfileService.java`
  Related Endpoints: Indirect backend flow triggered from `POST /api/auth/register`

- Feature: User profile CRUD and search
  Description: User profiles can be created, fetched, updated, and searched by display name. Profile responses include avatar URL, bio, last seen, and computed online state.
  Key Files: `user-service/src/main/java/com/orang/userservice/controller/ProfileController.java`, `user-service/src/main/java/com/orang/userservice/service/ProfileService.java`, `user-service/src/main/java/com/orang/userservice/entity/Profile.java`, `user-service/src/main/java/com/orang/userservice/repository/ProfileRepository.java`
  Related Endpoints: `GET /api/users/{userId}/profile`, `POST /api/users/{userId}/profile`, `PUT /api/users/{userId}/profile`, `GET /api/users/search`

- Feature: Contact management
  Description: The backend supports adding, listing, and removing contacts. Responses are enriched with contact profile metadata and Redis-backed online state.
  Key Files: `user-service/src/main/java/com/orang/userservice/controller/ContactController.java`, `user-service/src/main/java/com/orang/userservice/service/ContactService.java`, `user-service/src/main/java/com/orang/userservice/entity/Contact.java`, `user-service/src/main/java/com/orang/userservice/repository/ContactRepository.java`
  Related Endpoints: `POST /api/users/{userId}/contacts/{contactUserId}`, `GET /api/users/{userId}/contacts`, `DELETE /api/users/{userId}/contacts/{contactUserId}`

- Feature: Presence tracking
  Description: Online state is written to Redis from both explicit REST updates and WebSocket lifecycle events, then surfaced through profile and contact responses.
  Key Files: `user-service/src/main/java/com/orang/userservice/controller/ProfileController.java`, `user-service/src/main/java/com/orang/userservice/service/ProfileService.java`, `chat-service/src/main/java/com/orang/chatservice/event/WebSocketEventListener.java`
  Related Endpoints: `POST /api/users/{userId}/online`, indirect WebSocket connect/disconnect flow

- Feature: Conversation management
  Description: Users can list their conversations, open or reuse direct conversations, and create group conversations. Conversation membership is stored directly in the message-service data model.
  Key Files: `message-service/src/main/java/com/orang/messageservice/controller/ConversationController.java`, `message-service/src/main/java/com/orang/messageservice/service/ConversationService.java`, `message-service/src/main/java/com/orang/messageservice/entity/Conversation.java`, `message-service/src/main/java/com/orang/messageservice/repository/ConversationRepository.java`
  Related Endpoints: `GET /api/conversations`, `POST /api/conversations/direct/{targetUserId}`, `POST /api/conversations/group`

- Feature: Message history retrieval with access control
  Description: Message-service exposes paginated conversation history and enforces participant membership before returning persisted messages.
  Key Files: `message-service/src/main/java/com/orang/messageservice/controller/MessageController.java`, `message-service/src/main/java/com/orang/messageservice/service/MessageService.java`, `message-service/src/main/java/com/orang/messageservice/entity/Message.java`, `message-service/src/main/java/com/orang/messageservice/repository/MessageRepository.java`
  Related Endpoints: `GET /api/messages/{conversationId}`

- Feature: Realtime direct messaging and async persistence
  Description: Chat-service accepts STOMP messages, relays them to connected users, and publishes non-typing direct messages to RabbitMQ. Message-service consumes those events, creates or reuses a direct conversation, and persists the message.
  Key Files: `chat-service/src/main/java/com/orang/chatservice/controller/ChatController.java`, `chat-service/src/main/java/com/orang/chatservice/config/WebSocketConfig.java`, `chat-service/src/main/java/com/orang/chatservice/config/WebSocketSecurityConfig.java`, `message-service/src/main/java/com/orang/messageservice/listener/ChatMessageListener.java`, `message-service/src/main/java/com/orang/messageservice/service/MessageService.java`
  Related Endpoints: `WS /ws`, STOMP `SEND /app/chat.send`, indirect persistence consumed from `chat.message.sent`

- Feature: API gateway routing and aggregated OpenAPI surface
  Description: Gateway routes are configured for auth, user, message, conversation, and WebSocket traffic, and SpringDoc aggregation is wired for the REST services.
  Key Files: `api-gateway/src/main/resources/application.yml`, `auth-service/src/main/java/com/orang/authservice/config/OpenApiConfig.java`, `user-service/src/main/java/com/orang/userservice/config/OpenApiConfig.java`, `message-service/src/main/java/com/orang/messageservice/config/OpenApiConfig.java`
  Related Endpoints: `POST /api/auth/**`, `GET|POST|PUT|DELETE /api/users/**`, `GET /api/messages/**`, `GET|POST /api/conversations/**`, `WS /ws/**`, `GET /v3/api-docs`, `GET /swagger-ui.html`

- Feature: Shared exception and event contracts
  Description: Common API error handling, exception types, RabbitMQ constants, and shared event DTOs are extracted into a shared library reused across services.
  Key Files: `shared-library/src/main/java/com/orang/shared/exception/GlobalExceptionHandler.java`, `shared-library/src/main/java/com/orang/shared/event/UserRegisteredEvent.java`, `shared-library/src/main/java/com/orang/shared/event/MessageReceipt.java`, `shared-library/src/main/java/com/orang/shared/constants/RabbitMQConstants.java`
  Related Endpoints: Cross-cutting behavior across all backend services

## 🟡 In Progress

- Feature: Read receipt persistence and lifecycle
  Current State: Chat-service can receive a receipt message over STOMP and forward a shared `MessageReceipt` event. Message-service contains a `message_receipts` entity stub, which indicates planned persistence.
  Missing Parts: No embedded ID class is present, no receipt repository or service exists, no RabbitMQ listener consumes `chat.receipt.received`, and no REST/WebSocket query/update path exposes persisted receipt state.
  Key Files: `chat-service/src/main/java/com/orang/chatservice/controller/ChatController.java`, `shared-library/src/main/java/com/orang/shared/event/MessageReceipt.java`, `message-service/src/main/java/com/orang/messageservice/entity/MessageReceipt.java`

- Feature: Group chat delivery persistence parity
  Current State: Group conversations can be created, and chat-service supports `MessageType.GROUP` for WebSocket fan-out to a topic.
  Missing Parts: Message persistence currently goes through `ChatMessageListener`, which only derives a direct conversation from `senderId` and `recipientId`. There is no explicit persisted group-message flow tied to an existing group conversation ID.
  Key Files: `message-service/src/main/java/com/orang/messageservice/service/ConversationService.java`, `message-service/src/main/java/com/orang/messageservice/listener/ChatMessageListener.java`, `chat-service/src/main/java/com/orang/chatservice/controller/ChatController.java`, `chat-service/src/main/java/com/orang/chatservice/dto/MessageType.java`

- Feature: Automated test coverage for business behavior
  Current State: Each module has a `@SpringBootTest` application startup test and the project builds successfully enough to generate `target` output.
  Missing Parts: There are no controller tests, repository tests, messaging integration tests, security tests, or Testcontainers-based infrastructure tests for the actual backend behavior.
  Key Files: `auth-service/src/test/java/com/orang/authservice/AuthServiceApplicationTests.java`, `user-service/src/test/java/com/orang/userservice/UserServiceApplicationTests.java`, `message-service/src/test/java/com/orang/messageservice/MessageServiceApplicationTests.java`, `chat-service/src/test/java/com/orang/chatservice/ChatServiceApplicationTests.java`

- Feature: Backend documentation accuracy
  Current State: Auth API docs now include logout and refresh endpoints. Root README updated to reflect actual docker-compose behavior, correct service startup order, complete environment variable table, and removal of stale TODO notes. Feature status and backend implementation status docs updated to reflect current auth capabilities and actual database migration state.
  Missing Parts: API docs for user-service, message-service, and chat-service endpoints have not yet been written.
  Key Files: `README.md`, `docs/api/auth-api.md`, `docs/feature-status.md`, `docs/api/backend-implementation-status.md`, `docs/api/technical-debt.md`

## 🔴 Not Implemented

- Feature: Versioned database migrations
  Expected Components: Flyway or Liquibase dependency configuration, versioned migration scripts, startup migration execution, and removal of reliance on Hibernate `ddl-auto: update` in service configs.

- Feature: Media and attachment handling through MinIO
  Expected Components: Storage service abstraction, MinIO client integration, upload/download endpoints, object metadata persistence, and authorization rules for profile pictures or chat attachments.

- Feature: Dedicated read-receipt query/update API
  Expected Components: Receipt repository, entity ID model, service logic, consumer for receipt events, and endpoints or subscription model to inspect receipt state per message or conversation.

- Feature: CI/CD automation
  Expected Components: GitHub Actions or equivalent pipeline for build, test, image publishing, and deployment validation.

- Feature: Observability stack beyond basic actuator
  Expected Components: Metrics export, centralized logging, distributed tracing, dashboards, and alerting hooks.

- Feature: Secret management for deployed environments
  Expected Components: Externalized secret sources or Docker/Kubernetes secret integration instead of hardcoded or default credentials in local config.

## ⚠️ Technical Debt

- Issue: `displayName` is duplicated between auth-service and user-service
  Impact: Auth owns data that user-service treats as profile source-of-truth, which creates drift risk and muddles service boundaries.
  Suggested Fix: Remove `displayName` from auth-service post-registration contract, keep it only as seed data for the registration event, and make user-service the sole owner.

- Issue: Contact entity stores `contactUserId` as a raw UUID instead of a richer relation model
  Impact: Contact enrichment requires repeated manual profile lookups and makes JPA-level joins or fetch strategies harder.
  Suggested Fix: Introduce the planned relation noted in `docs/api/technical-debt.md`, likely through a profile-backed association or a clearer read model.

- Issue: Message receipt persistence is only a partial stub
  Impact: Receipt handling appears supported in realtime flow but has no durable backend state, which will cause feature inconsistency and user-facing confusion.
  Suggested Fix: Complete the receipt model with an embedded ID, repository, consumer, service methods, and retrieval/update APIs.

- Issue: README status is stale relative to current code
  Status: **Resolved.** README, auth API docs, feature status, and backend implementation status have been updated to reflect current logout/refresh endpoints, correct docker-compose behavior, accurate service startup order, and complete environment variable documentation.

- Issue: Test coverage is mostly limited to application context startup
  Impact: Regressions in security, RabbitMQ integration, WebSocket flows, and data rules can land without detection.
  Suggested Fix: Add focused unit tests, MVC tests, repository tests, and container-backed integration tests for cross-service workflows.

- Issue: `MessageController` computes `safeSize` but still builds `PageRequest` with the original `size`
  Impact: The intended page size cap to 100 is not actually enforced, which can lead to unexpectedly large queries.
  Suggested Fix: Build the `PageRequest` with `safeSize` instead of the raw request parameter.

## 🛠 DevOps / Infrastructure

- Task: Docker Compose local development stack with isolated service dependencies
  Status: Implemented for local development. Includes API gateway, auth-service, user-service, chat-service, message-service, three PostgreSQL instances, Redis, RabbitMQ with STOMP plugin, and MinIO.

- Task: Per-service containerization
  Status: Implemented. Dockerfiles exist for gateway and runtime services.

- Task: API gateway entrypoint and route aggregation
  Status: Implemented. Gateway routes REST and WebSocket traffic and exposes aggregated OpenAPI docs.

- Task: Environment-based service configuration
  Status: Implemented at basic level. Service URLs, datasource URLs, and broker hosts are externalized through environment variables.

- Task: Database schema migration strategy
  Status: In progress. Flyway is configured in all services (`flyway.enabled: true`) and `ddl-auto` is set to `validate` instead of `update`. However, no versioned migration scripts (`db/migration/V*.sql`) have been written yet — the schema is currently expected to be bootstrapped externally (e.g., by a previous Hibernate-managed run or a manual script). Migration scripts need to be created to complete the transition.

- Task: CI/CD pipeline
  Status: Missing.

- Task: Production-grade secret handling
  Status: Missing.

- Task: Monitoring, tracing, and centralized logs
  Status: Missing beyond actuator basics.

- Task: Integration testing infrastructure
  Status: Missing. No Testcontainers or equivalent test orchestration is present.

### GitHub Tasks

- [x] Implement user registration and login with JWT token issuance
- [x] Publish user registration events through RabbitMQ
- [x] Auto-create user profiles from registration events
- [x] Implement profile read, create, update, and search endpoints
- [x] Implement contact add, list, and delete flows
- [x] Track online presence with Redis and WebSocket lifecycle events
- [x] Implement conversation list, direct conversation creation, and group conversation creation
- [x] Implement paginated message history retrieval with participant authorization
- [x] Implement STOMP chat delivery with RabbitMQ-backed direct message persistence
- [x] Configure API gateway routing and aggregated OpenAPI docs
- [ ] Complete message read-receipt persistence and retrieval flow
- [ ] Add persisted group-message flow tied to group conversations
- [ ] Replace Hibernate `ddl-auto` schema management with versioned migrations
- [ ] Implement MinIO-backed media and attachment upload/download flows
- [ ] Add controller, service, repository, and messaging integration tests
- [ ] Add CI/CD pipeline for build, test, and image publishing
- [ ] Add observability stack for metrics, tracing, and centralized logging
- [ ] Externalize secrets for non-local environments
- [x] Update project documentation to reflect the current backend implementation status
- [ ] Refactor duplicated profile ownership concerns between auth-service and user-service
- [ ] Refactor contact/profile modeling to reduce manual lookup logic
- [ ] Fix message history page-size cap enforcement in `MessageController`