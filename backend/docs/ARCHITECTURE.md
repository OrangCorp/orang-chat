# Orang Chat Backend - Architecture & Design Patterns

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Design Patterns](#design-patterns)
3. [Data Flow](#data-flow)
4. [API Gateway Design](#api-gateway-design)
5. [Service Layer Architecture](#service-layer-architecture)
6. [Database Design](#database-design)
7. [Message Queue Architecture](#message-queue-architecture)
8. [Security Architecture](#security-architecture)
9. [Resilience & Fault Tolerance](#resilience--fault-tolerance)
10. [Deployment Architecture](#deployment-architecture)

---

## System Architecture

### High-Level Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           ORANG CHAT SYSTEM                              │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   CLIENT LAYER                                  │   │
│  │  Web Browser (Vite React) + WebSocket Capability               │   │
│  └─────────────────┬───────────────────────────────────────────────┘   │
│                    │                                                    │
│                    ▼                                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              EDGE / LOAD BALANCER                               │   │
│  │         (HAProxy / Nginx in production)                         │   │
│  └─────────────────┬───────────────────────────────────────────────┘   │
│                    │                                                    │
│                    ▼                                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │           API GATEWAY (Spring Cloud Gateway)                    │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │   │
│  │  │ Rate    │  │ Circuit │  │ Request │  │ OpenAPI │            │   │
│  │  │ Limiter │  │ Breaker │  │ Logger  │  │ Docs    │            │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘            │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └─────────┬──────────────┬──────────────┬───────────────────────┘   │
│            │              │              │                           │
│      ┌─────▼────┐  ┌──────▼──────┐ ┌────▼───────┐                  │
│      │   HTTP   │  │    HTTP     │ │  WebSocket │                  │
│      │ REST API │  │   Routing   │ │   Upgrade  │                  │
│      └──────────┘  └─────────────┘ └────────────┘                  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              MICROSERVICES (Per-service DB)                   │ │
│  ├────────────────────────────────────────────────────────────────┤ │
│  │                                                                │ │
│  │  ┌──────────────────┐  ┌──────────────────┐                  │ │
│  │  │  Auth Service    │  │  User Service    │                  │ │
│  │  │  Port: 8081      │  │  Port: 8082      │                  │ │
│  │  ├──────────────────┤  ├──────────────────┤                  │ │
│  │  │ • Register       │  │ • Profiles       │                  │ │
│  │  │ • Login/Logout   │  │ • Contacts       │                  │ │
│  │  │ • JWT Generation │  │ • Presence       │                  │ │
│  │  │ • Email Verify   │  │ • Search         │                  │ │
│  │  │ • Password Reset │  │ • Sessions       │                  │ │
│  │  ├──────────────────┤  ├──────────────────┤                  │ │
│  │  │   auth_db        │  │    user_db       │                  │ │
│  │  │  PostgreSQL      │  │  PostgreSQL      │                  │ │
│  │  │  :5432           │  │  :5433           │                  │ │
│  │  └──────────────────┘  └──────────────────┘                  │ │
│  │                                                                │ │
│  │  ┌──────────────────┐  ┌──────────────────┐                  │ │
│  │  │  Chat Service    │  │  Message Service │                  │ │
│  │  │  Port: 8083      │  │  Port: 8084      │                  │ │
│  │  ├──────────────────┤  ├──────────────────┤                  │ │
│  │  │ • WebSocket/STOMP│  │ • Message CRUD   │                  │ │
│  │  │ • Presence       │  │ • Conversations  │                  │ │
│  │  │ • RabbitMQ Relay │  │ • Reactions      │                  │ │
│  │  │ • Broadcast      │  │ • Pins, Reads    │                  │ │
│  │  │                  │  │ • Attachments    │                  │ │
│  │  ├──────────────────┤  │ • Full-text      │                  │ │
│  │  │  Redis Only      │  │   Search         │                  │ │
│  │  │  (Lettuce)       │  │ • Thumbnails     │                  │ │
│  │  │  :6379           │  ├──────────────────┤                  │ │
│  │  └──────────────────┘  │  message_db      │                  │ │
│  │                        │  PostgreSQL      │                  │ │
│  │  ┌──────────────────┐  │  :5434           │                  │ │
│  │  │ Notification Srv │  └──────────────────┘                  │ │
│  │  │  Port: 8085      │                                        │ │
│  │  ├──────────────────┤  ┌──────────────────┐                  │ │
│  │  │ • Web Push       │  │ Shared Library   │                  │ │
│  │  │ • VAPID          │  ├──────────────────┤                  │ │
│  │  │ • Subscriptions  │  │ • JWT Utils      │                  │ │
│  │  │ • Preferences    │  │ • Security       │                  │ │
│  │  │ • Push Delivery  │  │ • Events         │                  │ │
│  │  ├──────────────────┤  │ • DTOs           │                  │ │
│  │  │ notification_db  │  │ • Constants      │                  │ │
│  │  │  PostgreSQL      │  └──────────────────┘                  │ │
│  │  │  :5435           │                                        │ │
│  │  └──────────────────┘                                        │ │
│  │                                                                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                           │                                          │
│                ┌──────────┼──────────┐                               │
│                │          │          │                               │
│                ▼          ▼          ▼                               │
│  ┌──────────────────┐ ┌────────────────────┐ ┌──────────────────┐  │
│  │     RabbitMQ     │ │  Redis Cache       │ │   MinIO Storage  │  │
│  │  Message Broker  │ │  :6379             │ │   S3-compatible  │  │
│  │  :5672           │ │  - Sessions        │ │   :9000          │  │
│  │  - Events        │ │  - Cache           │ │  - Attachments   │  │
│  │  - Topics        │ │  - Token Blacklist │ │  - Thumbnails    │  │
│  │  - Exchanges     │ │  - ShedLock        │ │                  │  │
│  └──────────────────┘ └────────────────────┘ └──────────────────┘  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Design Patterns

### 1. **Microservices Pattern**
- **Each service**: Independent deployment, owns data, single responsibility
- **Communication**: Synchronous (REST) or asynchronous (RabbitMQ)
- **Data consistency**: Eventual via event sourcing

### 2. **API Gateway Pattern**
- **Single entry point**: Clients talk to gateway, not services
- **Routing**: Route to correct backend based on path
- **Cross-cutting concerns**: Rate limiting, CORS, auth validation
- **API aggregation**: Combine OpenAPI docs from all services

### 3. **Event-Driven Pattern**
- **Publishers**: Services emit events when important actions occur
- **Subscribers**: Services listen and react to events
- **Loose coupling**: Publishers don't know subscribers
- **Eventual consistency**: Events processed asynchronously

### 4. **Saga Pattern (Distributed Transactions)**
Problem: "Register user" spans Auth DB + User DB, but no distributed transaction
Solution: Event choreography
```
1. Auth Service: creates user → emits UserRegisteredEvent
2. User Service: listens → creates profile idempotently
3. If User Service crashes, message waits in queue
4. When User Service restarts, profile is created (eventual consistency ✓)
```

### 5. **Repository Pattern**
- **Spring Data JPA**: Abstracts database access
- **Type-safe queries**: No raw SQL in business logic
- **Testable**: Can mock repositories for unit tests

### 6. **Service Layer Pattern**
- **Controller**: HTTP request/response
- **Service**: Business logic (validation, authorization)
- **Repository**: Data access
- **Benefit**: Testable business logic independent of HTTP

### 7. **JWT (Stateless) Authentication**
- **Token Structure**: Contains userId, issued at, expiration
- **Verification**: HMAC signature proves server created token
- **Advantage**: No session replication across services
- **Trade-off**: Can't revoke token until expiration (mitigated with blacklist)

### 8. **Circuit Breaker Pattern** (Ready for implementation)
```
// If Message Service is down:
@CircuitBreaker(name = "messageService")
public List<Message> getMessages(...) {
  // Calls Message Service
  // If repeated failures, circuit OPENS
  // Requests fail fast instead of hanging
  // Prevents cascading failures
}
```

### 9. **Bulkhead Pattern** (Ready for implementation)
```
// Isolate thread pools per service
@Bulkhead(name = "getMessages", type = Bulkhead.Type.THREAD)
public List<Message> getMessages(...) {
  // Dedicates thread pool for this operation
  // If getMessages slow, won't block other operations
}
```

### 10. **Retry Pattern** (Active in Thumbnail Generation)
```
@Retry(name = "thumbnailGeneration", maxAttempts = 3)
@Async
public void generateThumbnail(Attachment attachment) {
  // Retries up to 3 times on failure
  // Exponential backoff between retries
}
```

---

## Data Flow

### User Registration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ CLIENT (Web Browser)                                            │
│  POST /api/auth/register                                       │
│  {email, password, displayName}                                │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ API GATEWAY                                                     │
│  1. Validate JWT (not present for register ✓)                   │
│  2. Rate limit: 5 req/sec by IP address                         │
│  3. Forward to Auth Service                                     │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ AUTH SERVICE - AuthController                                   │
│  1. Validate email format & not exists                          │
│  2. Hash password with BCrypt                                   │
│  3. Call AuthService.register()                                 │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ AUTH SERVICE - AuthService                                      │
│  1. Save User to auth_db                                        │
│  2. Generate email verification code (6 digits)                 │
│  3. Store code in Redis with 15-min TTL                         │
│  4. Send verification email via SMTP                            │
│  5. BEGIN TRANSACTION                                           │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ AUTH SERVICE - PostgreSQL auth_db                               │
│  INSERT INTO users (id, email, passwordHash, displayName, ...)  │
│  COMMIT TRANSACTION                                             │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼ (After DB Commit)
┌─────────────────────────────────────────────────────────────────┐
│ AUTH SERVICE - Spring Transactions                              │
│  Publisher detects @TransactionalEventListener                  │
│  Publishes UserRegisteredEvent to RabbitMQ                      │
│  {userId, email, displayName, timestamp}                        │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ RABBITMQ - Topic Exchange                                       │
│  user.exchange (topic) receives event                           │
│  Routes to user.registered.queue (durable)                      │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ USER SERVICE - RabbitMQ Listener                                │
│  1. Receives UserRegisteredEvent                                │
│  2. Calls ProfileService.createProfileFromEvent()               │
│  3. Check idempotency: if profile exists, skip                  │
│  4. Create new Profile (displayName from event)                 │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ USER SERVICE - PostgreSQL user_db                               │
│  INSERT INTO profiles (userId, displayName, ...)                │
│  COMMIT                                                         │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼ (Response to client)
┌─────────────────────────────────────────────────────────────────┐
│ CLIENT - Browser                                                │
│  200 OK                                                         │
│  {userId, email, displayName, accessToken, refreshToken,       │
│   tokenType: "Bearer", expiresIn: 900}                          │
│                                                                 │
│  Result:                                                        │
│  ✓ User registered in auth_db                                   │
│  ✓ Profile created in user_db (eventually)                      │
│  ✓ Email sent with verification code                            │
│  ✓ JWT tokens ready for API calls                               │
└─────────────────────────────────────────────────────────────────┘
```

### Message Send Flow (WebSocket)

```
┌──────────────────────────────────────────────────────────────────┐
│ CLIENT - Web Browser                                             │
│ WebSocket connected to /ws (Chat Service via Gateway)            │
│ SEND /app/chat.send                                              │
│ ChatMessagePayload {                                             │
│   type: "GROUP",                                                 │
│   conversationId: uuid,                                          │
│   senderId: uuid,                                                │
│   content: "Hello everyone!",                                    │
│   attachmentIds: [...],                                          │
│   replyToMessageId: optional,                                    │
│   mentions: [uuid, ...]                                          │
│ }                                                                │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ GATEWAY - WebSocket Handler                                      │
│  1. JWT validation from initial CONNECT handshake                │
│  2. Extract userId from JWT                                      │
│  3. Forward SEND to Chat Service instance                        │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ CHAT SERVICE - ChatController (@MessageMapping)                  │
│  1. Receive payload                                              │
│  2. Validate payload (not null, conversationId present for GROUP) │
│  3. Publish to RabbitMQ chat.exchange (key: chat.message.sent)   │
│  4. SIMULTANEOUSLY broadcast to local /topic/conversation/{id}   │
│  5. Connected clients receive message (same Chat Service)        │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼ (Topic Exchange)
┌──────────────────────────────────────────────────────────────────┐
│ RABBITMQ                                                         │
│  chat.exchange (topic) receives ChatMessagePayload               │
│  Routes to chat.message.sent.queue (durable)                     │
│                                                                  │
│  Also received by:                                               │
│  - Other Chat Service instances (for inter-instance broadcast)   │
│  - Message Service (for persistence)                             │
│  - Notification Service (for push notifications)                 │
└────────┬──────────────┬──────────────┬──────────────────────────┘
         │              │              │
         ▼              ▼              ▼
    OTHER CHATS     MESSAGE SERVICE   NOTIFICATION SERVICE
    INSTANCES       PERSISTENCE       PUSH DELIVERY
    
┌──────────────────────────────────────────────────────────────────┐
│ MESSAGE SERVICE - ChatMessageListener                            │
│  1. Receives ChatMessagePayload from queue                       │
│  2. Create Message entity from payload                           │
│  3. Link attachments if present                                  │
│  4. Save to message_db                                           │
│  5. BEGIN TRANSACTION & FLUSH                                    │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ MESSAGE SERVICE - PostgreSQL message_db                          │
│  INSERT INTO messages (conversationId, senderId, content, ...)   │
│  UPDATE attachments SET messageId = ... (FK link)                │
│  INSERT INTO message_mentions (messageId, mentionedUserId, ...)  │
│  COMMIT                                                          │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼ (After DB Commit)
┌──────────────────────────────────────────────────────────────────┐
│ MESSAGE SERVICE - Spring Transactions                            │
│  Publisher detects @TransactionalEventListener                   │
│  1. Publishes MessageSentEvent                                   │
│  2. Publishes MentionEvent for each @mentioned user              │
│  3. If attachments, publishes ThumbnailRequestedEvent            │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼ (For each Thumbnail)
┌──────────────────────────────────────────────────────────────────┐
│ NOTIFICATION SERVICE - ThumbnailListener                         │
│  1. Downloads image from MinIO                                   │
│  2. Generates thumbnail with Thumbnailator                       │
│  3. Uploads thumbnail to MinIO                                   │
│  4. Publishes ThumbnailReadyEvent                                │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ MESSAGE SERVICE - Receives ThumbnailReadyEvent                   │
│  1. Updates attachment record with thumbnailKey                  │
│  2. Publishes THUMBNAIL_READY WebSocket message                  │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼ (RabbitMQ)
┌──────────────────────────────────────────────────────────────────┐
│ NOTIFICATION SERVICE - Receives MessageSentEvent & MentionEvent  │
│  1. Query user notification preferences                          │
│  2. Get push subscriptions for all conversation members          │
│  3. Send Web Push with payload                                   │
│  4. Browser receives notification (native OS notification)       │
└────────┬─────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────┐
│ RESULTS:                                                         │
│ ✓ Message persisted in message_db                                │
│ ✓ All WebSocket clients see message in real-time                 │
│ ✓ All users get push notification (if subscribed + not muted)    │
│ ✓ Thumbnails generated asynchronously                            │
│ ✓ No blocking of message send                                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## API Gateway Design

### Gateway Responsibilities

```
┌─────────────────────────────────────────────────────────────┐
│             Spring Cloud Gateway                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 1. REQUEST ROUTING                                          │
│    GET  /api/auth/**    → http://auth-service:8081         │
│    GET  /api/users/**   → http://user-service:8082         │
│    GET  /api/messages/**→ http://message-service:8084       │
│    GET  /api/push/**    → http://notification-service:8085 │
│    WS   /ws             → ws://chat-service:8083           │
│                                                             │
│ 2. RATE LIMITING (RedisRateLimiter)                         │
│    Auth Service:    5 req/s, burst 10 (by IP)              │
│    User Service:    20 req/s, burst 40 (by user)           │
│    Message Service: 50 req/s, burst 100 (by user)          │
│    Notification:    10 req/s, burst 20 (by user)           │
│                                                             │
│ 3. JWT VALIDATION (JWT Filter)                              │
│    Extract JWT from Authorization header                    │
│    Validate signature                                       │
│    Set spring-cloud-security context                        │
│    Skip for: /api/auth/register, /api/auth/login,           │
│              /actuator/health, /swagger-ui                  │
│                                                             │
│ 4. CIRCUIT BREAKER (Optional)                               │
│    If backend service down:                                 │
│    - Close circuit (fast fail)                              │
│    - Return 503 Service Unavailable                         │
│    - Try again after cool-down period                       │
│                                                             │
│ 5. CORS HANDLING                                            │
│    Allow origins: https://yourdomain.com                    │
│    Allow methods: GET, POST, PUT, DELETE, OPTIONS           │
│    Allow headers: Authorization, Content-Type               │
│    Allow credentials: true                                  │
│                                                             │
│ 6. OPENAPI AGGREGATION                                      │
│    GET /v3/api-docs → Combines all service OpenAPI docs     │
│    GET /swagger-ui  → Interactive API documentation         │
│                                                             │
│ 7. REQUEST/RESPONSE LOGGING                                 │
│    Log: path, method, status, response time                 │
│    Debug: request/response bodies (configurable)            │
│                                                             │
│ 8. TIMEOUT MANAGEMENT                                       │
│    Connect timeout: 5s                                      │
│    Response timeout: 30s (override per route)               │
│    Read/Write timeout: 30s                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Gateway Route Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth Service
        - id: auth_service
          uri: http://auth-service:8081
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/auth(/?.*),$1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 5
                redis-rate-limiter.burstCapacity: 10
                key-resolver: "#{@ipAddressKeyResolver}"

        # User Service
        - id: user_service
          uri: http://user-service:8082
          predicates:
            - Path=/api/users/**,/api/contacts/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
                key-resolver: "#{@userIdKeyResolver}"
            - JwtAuthenticationFilter

        # Chat Service (WebSocket)
        - id: chat_service
          uri: ws://chat-service:8083
          predicates:
            - Path=/ws/**
          filters:
            - RewritePath=/ws(/?.*),$1

        # Message Service
        - id: message_service
          uri: http://message-service:8084
          predicates:
            - Path=/api/messages/**,/api/conversations/**,/api/reactions/**,/api/attachments/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
                key-resolver: "#{@userIdKeyResolver}"
            - JwtAuthenticationFilter

        # Notification Service
        - id: notification_service
          uri: http://notification-service:8085
          predicates:
            - Path=/api/push/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userIdKeyResolver}"
            - JwtAuthenticationFilter
```

---

## Service Layer Architecture

### Service Structure Pattern

Each microservice follows this layered architecture:

```
com.orang.{service}/
├── controller/            # HTTP endpoints
│   ├── AuthController
│   ├── UserController
│   └── ...
│
├── service/               # Business logic
│   ├── AuthService
│   ├── EmailService
│   ├── JwtService
│   └── ...
│
├── repository/            # Data access (Spring Data JPA)
│   ├── UserRepository
│   ├── ContactRepository
│   └── ...
│
├── entity/                # JPA entities
│   ├── User
│   ├── Contact
│   └── ...
│
├── dto/                   # Request/Response DTOs
│   ├── RegisterRequest
│   ├── LoginRequest
│   ├── AuthResponse
│   └── ...
│
├── event/                 # RabbitMQ event models
│   ├── UserRegisteredEvent
│   ├── MessageSentEvent
│   └── ...
│
├── listener/              # RabbitMQ event listeners
│   ├── UserRegisteredEventListener
│   ├── ChatMessageListener
│   └── ...
│
├── security/              # Security components
│   ├── JwtAuthenticationFilter
│   ├── JwtUtils (in shared-library)
│   └── SecurityConfig
│
├── exception/             # Custom exceptions
│   ├── AuthException
│   ├── ResourceNotFoundException
│   └── ...
│
├── config/                # Spring configuration
│   ├── RabbitMQConfig
│   ├── WebSocketConfig
│   └── SecurityConfig
│
├── mapper/                # DTO <-> Entity mapping
│   ├── UserMapper
│   └── ...
│
└── resources/
    ├── application.yaml   # Spring Boot configuration
    └── db/migration/      # Flyway SQL migrations
        ├── V1__init.sql
        ├── V2__add_feature.sql
        └── ...
```

### Example Service Implementation

```java
@Service
@Transactional
public class UserService {
    
    // 1. Dependency injection
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    
    // 2. Business logic methods
    public UserProfile createProfile(UUID userId, CreateProfileRequest req) {
        // Validate authorization
        validateUserOwnership(userId);
        
        // Check if profile exists (idempotency)
        Optional<UserProfile> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get());
        }
        
        // Create new profile
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName(req.getDisplayName());
        profile.setAvatarUrl(req.getAvatarUrl());
        profile.setBio(req.getBio());
        profile.setCreatedAt(Instant.now());
        
        // Save to database
        UserProfile saved = userRepository.save(profile);
        
        // Cache in Redis (1 hour TTL)
        cacheProfile(saved);
        
        return mapper.toResponse(saved);
    }
    
    public void removeContact(UUID userId, UUID contactId) {
        // Authorization: user must be party to contact
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        
        if (!contact.involvesUser(userId)) {
            throw new AuthorizationException("User not involved in contact");
        }
        
        // Soft delete (set deletedAt timestamp)
        contact.setDeletedAt(Instant.now());
        contactRepository.save(contact);
        
        // Publish event
        rabbitTemplate.convertAndSend("user.exchange", "contact.removed",
            new ContactRemovedEvent(contactId, userId));
    }
    
    // 3. Cache management
    private void cacheProfile(UserProfile profile) {
        String key = "profile:" + profile.getUserId();
        redisTemplate.opsForValue().set(key, profile, Duration.ofHours(1));
    }
    
    // 4. Authorization checks
    private void validateUserOwnership(UUID userId) {
        String currentUserId = SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        if (!currentUserId.equals(userId.toString())) {
            throw new AuthorizationException("User cannot access other users' data");
        }
    }
}
```

---

## Database Design

### Entity-Relationship Diagram (Simplified)

```
Auth DB (auth_db):
┌──────────────────────────┐
│ users                    │
├──────────────────────────┤
│ id (UUID, PK)            │
│ email (unique, string)   │
│ passwordHash (BCrypt)    │
│ displayName (string)     │
│ emailVerified (bool)     │
│ createdAt (timestamp)    │
│ updatedAt (timestamp)    │
└──────────────────────────┘

User DB (user_db):
┌──────────────────────────┐
│ profiles                 │
├──────────────────────────┤
│ userId (UUID, PK, FK)    │
│ displayName (string)     │ ← Can differ from auth.users.displayName
│ avatarUrl (string)       │
│ bio (text)               │
│ lastSeen (timestamp)     │
│ createdAt (timestamp)    │
│ updatedAt (timestamp)    │
└──────────────────────────┘

┌──────────────────────────┐
│ contacts                 │
├──────────────────────────┤
│ id (UUID, PK)            │
│ requesterId (UUID, FK)   │ ──┐
│ recipientId (UUID, FK)   │  ├─ Both reference users (auth_db)
│ status (enum)            │ ──┘
│   PENDING/ACCEPTED/BLOCKED
│ acceptedAt (timestamp)   │
│ createdAt (timestamp)    │
│ deletedAt (timestamp)    │ ← Soft delete
└──────────────────────────┘

Message DB (message_db):
┌──────────────────────────┐
│ conversations            │
├──────────────────────────┤
│ id (UUID, PK)            │
│ name (string, optional)  │ ← NULL for direct chats
│ isGroup (boolean)        │
│ createdBy (UUID)         │
│ createdAt (timestamp)    │
└──────────────────────────┘

┌────────────────────────────────────┐
│ conversation_participants          │
├────────────────────────────────────┤
│ conversationId (UUID, PK, FK)      │
│ userId (UUID, PK, FK)              │
│ role (enum: MEMBER/ADMIN)          │
│ joinedAt (timestamp)               │
│ leftAt (timestamp, nullable)       │ ← Soft delete
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ messages                           │
├────────────────────────────────────┤
│ id (UUID, PK)                      │
│ conversationId (UUID, FK)          │ ──┐
│ senderId (UUID)                    │   ├─ FK to participants
│ content (text, 2000 char max)      │ ──┘
│ replyToMessageId (UUID, FK, nullable)
│ createdAt (timestamp)              │
│ editedAt (timestamp, nullable)     │
│ deletedAt (timestamp, nullable)    │ ← Soft delete
│ INDEX: full-text GIN on content    │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ message_reactions                  │
├────────────────────────────────────┤
│ messageId (UUID, PK, FK)           │
│ userId (UUID, PK)                  │
│ reactionType (enum: LIKE/ORANG)    │
│ createdAt (timestamp)              │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ pinned_messages                    │
├────────────────────────────────────┤
│ id (UUID, PK)                      │
│ conversationId (UUID, FK)          │
│ messageId (UUID, FK)               │
│ pinnedBy (UUID)                    │
│ pinnedAt (timestamp)               │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ read_receipts                      │
├────────────────────────────────────┤
│ messageId (UUID, PK, FK)           │
│ userId (UUID, PK)                  │
│ readAt (timestamp)                 │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ attachments                        │
├────────────────────────────────────┤
│ id (UUID, PK)                      │
│ messageId (UUID, FK)               │
│ fileType (string: image/jpeg, etc) │
│ fileSize (bigint: bytes)           │
│ s3Key (string: path in MinIO)      │
│ thumbnailKey (string, nullable)    │
│ thumbnailStatus (enum)             │
│   PENDING/READY/FAILED             │
│ createdAt (timestamp)              │
└────────────────────────────────────┘

Notification DB (notification_db):
┌────────────────────────────────────┐
│ push_subscriptions                 │
├────────────────────────────────────┤
│ id (UUID, PK)                      │
│ userId (UUID, FK to auth_db)       │
│ endpoint (string, unique)          │
│   https://fcm.googleapis.com/...   │
│ p256dhKey (string: ECDP256 key)    │
│ authKey (string: authentication)   │
│ expiresAt (timestamp, nullable)    │
│ userAgent (string: device info)    │
│ createdAt (timestamp)              │
│ lastUsedAt (timestamp)             │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ notification_preferences           │
├────────────────────────────────────┤
│ userId (UUID, PK, FK to auth_db)   │
│ conversationId (UUID, PK, FK)      │
│ muted (boolean: true = silent)     │
│ mutedUntil (timestamp, nullable)   │
│   (e.g., mute for 1 hour)          │
│ createdAt (timestamp)              │
│ updatedAt (timestamp)              │
└────────────────────────────────────┘
```

### Query Patterns & Indexes

```sql
-- User lookups (Auth Service)
CREATE INDEX idx_users_email ON users(email);

-- Conversation queries
CREATE INDEX idx_conversation_participants_user ON conversation_participants(userId);
CREATE INDEX idx_messages_conversation ON messages(conversationId);

-- Search queries (Message Service)
CREATE INDEX idx_messages_content_gin ON messages 
  USING GIN(to_tsvector('english', content));

-- Read receipt tracking
CREATE INDEX idx_read_receipts_user ON read_receipts(userId);
CREATE INDEX idx_read_receipts_message ON read_receipts(messageId);

-- Contact queries (User Service)
CREATE INDEX idx_contacts_requester ON contacts(requesterId, status);
CREATE INDEX idx_contacts_recipient ON contacts(recipientId, status);
```

---

## Message Queue Architecture

### RabbitMQ Configuration

```
Exchanges:
  ├─ user.exchange (topic)
  │  └─ Routes user-related events (registration, updates)
  │
  ├─ contact.exchange (topic)
  │  └─ Routes contact-related events (requests, acceptance, blocking)
  │
  ├─ chat.exchange (topic)
  │  └─ Routes real-time messages
  │     (received by multiple Chat Service instances + Message Service)
  │
  ├─ message.exchange (topic)
  │  └─ Routes message mutations (sent, edited, deleted, reacted, pinned)
  │
  ├─ attachment.exchange (topic)
  │  └─ Routes attachment events (thumbnail requested, ready)
  │
  └─ notification.exchange (topic)
     └─ Routes notification events (push delivery)

Queues:
  ├─ user.registered.queue → User Service (creates profile)
  │
  ├─ contact.request.queue → Notification Service (sends push)
  │
  ├─ chat.message.sent.queue → Message Service (persists)
  │                          → Notification Service (pushes)
  │                          → (Other Chat Service instances receive via topic)
  │
  ├─ attachment.thumbnail.queue → Notification Service (generates thumbnail)
  │
  ├─ message.sent.queue → Notification Service (delivers push)
  │
  └─ (Many more for specific events...)
```

### Event Publishing Pattern

```java
// In service layer, after DB transaction commits:
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Transactional
    public User register(RegisterRequest req) {
        // 1. Save to database
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        User saved = userRepository.save(user);
        
        // 2. After transaction commits, Spring detects and publishes event
        return saved;
        
        // Note: No explicit publish call needed!
        // Spring's transaction management handles this.
    }
    
    // Event published automatically after save() commits:
    @Bean
    public ApplicationEventPublisher.EventPublishingStrategy strategy() {
        // RabbitTemplate configured to send to RabbitMQ via Jackson2JsonMessageConverter
        return event -> rabbitTemplate.convertAndSend(
            "user.exchange",
            "user.registered",
            event
        );
    }
}
```

### Event Consumption Pattern

```java
@Service
public class UserProfileService {
    
    private final ProfileRepository profileRepository;
    
    // Listen to user registration events from Auth Service
    @RabbitListener(queues = "user.registered.queue")
    public void onUserRegistered(UserRegisteredEvent event) {
        // Check idempotency
        Optional<Profile> existing = profileRepository.findById(event.getUserId());
        if (existing.isPresent()) {
            log.debug("Profile already exists for user: {}", event.getUserId());
            return;
        }
        
        // Create profile
        Profile profile = new Profile();
        profile.setUserId(event.getUserId());
        profile.setDisplayName(event.getDisplayName());
        profile.setCreatedAt(Instant.now());
        
        profileRepository.save(profile);
        log.info("Profile created for user: {}", event.getUserId());
        
        // If exception thrown here, message goes to DLQ (Dead Letter Queue)
        // Can be replayed later
    }
}
```

---

## Security Architecture

### JWT Token Flow

```
┌──────────────────────────────────────────────────────────────────┐
│ REGISTRATION / LOGIN                                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 1. Client sends credentials                                     │
│    POST /api/auth/login {email, password}                       │
│                                                                  │
│ 2. Auth Service validates password (BCrypt compare)             │
│                                                                  │
│ 3. If valid, generates two JWT tokens:                          │
│                                                                  │
│    ACCESS TOKEN (short-lived):                                  │
│    ├─ Header: {alg: "HS256", typ: "JWT"}                        │
│    ├─ Payload: {sub: userId, email, iat, exp}                  │
│    ├─ Signature: HMAC-SHA256(header.payload, secret)            │
│    └─ TTL: 15 minutes                                           │
│                                                                  │
│    REFRESH TOKEN (long-lived):                                  │
│    ├─ Header: {alg: "HS256", typ: "JWT"}                        │
│    ├─ Payload: {sub: userId, jti (token ID), iat, exp}         │
│    ├─ Signature: HMAC-SHA256(header.payload, secret)            │
│    └─ TTL: 7 days                                               │
│                                                                  │
│ 4. Server responds with both tokens                             │
│    200 OK {accessToken, refreshToken, expiresIn: 900}           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ API CALL WITH JWT                                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 1. Client includes access token in every request                │
│    GET /api/messages/conversation/{id}                          │
│    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...               │
│                                                                  │
│ 2. API Gateway extracts token from Authorization header         │
│                                                                  │
│ 3. Gateway verifies signature:                                  │
│    1. Split token into [header, payload, signature]             │
│    2. Recompute: HMAC-SHA256(header.payload, secret)            │
│    3. Compare with signature from token                         │
│    4. If mismatch: INVALID TOKEN ✗                              │
│    5. If match: TOKEN VALID ✓                                   │
│                                                                  │
│ 4. Gateway checks expiration                                    │
│    1. Decode payload: {exp: 1714302600}                         │
│    2. If now > exp: TOKEN EXPIRED ✗                             │
│    3. If now < exp: TOKEN VALID ✓                               │
│                                                                  │
│ 5. Gateway extracts userId from payload (sub claim)             │
│    sub: "550e8400-e29b-41d4-a716-446655440000"                 │
│                                                                  │
│ 6. Gateway forwards to backend service                          │
│    + X-User-Id: 550e8400-e29b-41d4-a716-446655440000 (header)  │
│                                                                  │
│ 7. Backend service injects userId via @AuthenticationPrincipal  │
│    public void getMessage(@AuthenticationPrincipal String userId) │
│                                                                  │
│ 8. Backend uses userId for authorization (ownership checks)     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ TOKEN REFRESH (Extending Session)                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 1. Client detects access token expiring soon                    │
│                                                                  │
│ 2. Client sends refresh token                                   │
│    POST /api/auth/refresh {refreshToken: "eyJ..."}              │
│                                                                  │
│ 3. Auth Service verifies refresh token signature & expiration    │
│                                                                  │
│ 4. Auth Service checks for reuse (Redis marker):                │
│    ├─ Key: "token:{jti}:used"                                   │
│    ├─ If exists: TOKEN REUSED (security breach!)                │
│    │   └─ Blacklist entire user: "user:{userId}:blacklist"     │
│    │   └─ Return 401 Unauthorized                               │
│    └─ If not exists: Fresh token                                │
│                                                                  │
│ 5. If fresh:                                                    │
│    ├─ Store marker: SET "token:{jti}:used" (TTL = refresh exp)  │
│    ├─ Generate new access + refresh tokens                      │
│    ├─ Return both tokens                                        │
│                                                                  │
│ 6. Client stores new tokens, discards old refresh token         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ TOKEN BLACKLIST (Logout)                                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 1. User clicks Logout                                           │
│    POST /api/auth/logout                                        │
│    Authorization: Bearer {accessToken}                          │
│                                                                  │
│ 2. Auth Service extracts userId from token                      │
│                                                                  │
│ 3. Auth Service stores in Redis:                                │
│    SET "user:{userId}:blacklist" true                           │
│    EXPIRE {refreshTokenTTL}  ← 7 days                           │
│                                                                  │
│ 4. Any subsequent request with that user's tokens               │
│    is rejected (checked by every service)                       │
│                                                                  │
│ 5. After 7 days, blacklist key expires (Redis TTL)              │
│    → Tokens can no longer be reused anyway (naturally expired)   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Authorization Patterns

```
1. RESOURCE OWNERSHIP CHECK
   User A tries to edit User B's profile
   → 403 Forbidden (User B owns that resource)

2. CONVERSATION MEMBERSHIP CHECK
   User X (not member) tries to see conversation messages
   → 403 Forbidden (User X not participant)

3. CONTACT STATUS CHECK
   User A (blocked by User B) tries to message User B
   → 403 Forbidden (contact.status = BLOCKED)

4. ROLE-BASED CHECK
   User A (MEMBER) tries to remove User B from group
   → 403 Forbidden (only ADMIN can remove members)

5. OWNERSHIP + AUTHORIZATION COMBINED
   User A tries to delete message from User B
   → 403 Forbidden (User A not sender, not admin)
```

---

## Resilience & Fault Tolerance

### Failure Scenarios & Handling

```
SCENARIO 1: Message Service Down
├─ Chat Service publishes message to RabbitMQ
├─ Message waits in queue (durable)
├─ Client still receives message via STOMP (no wait)
├─ When Message Service restarts
├─ Queue replayed: messages persisted
└─ ✓ Message not lost

SCENARIO 2: Notification Service Down
├─ Message Service publishes MessageSentEvent
├─ Event queued in RabbitMQ (durable)
├─ Chat continues working (no blocking)
├─ When Notification Service restarts
├─ Queue replayed: push notifications sent (possibly delayed)
└─ ✓ No push spam (exactly-once delivery via RabbitMQ manual ACK)

SCENARIO 3: Auth Service Down
├─ Gateway has cached JWT validation (optional)
├─ If cache enabled: can use stale token validity
├─ If cache disabled: return 503 Service Unavailable
└─ ⚠ Mitigated with circuit breaker pattern

SCENARIO 4: Database Connection Pool Exhausted
├─ Service tries to acquire connection
├─ No available connections in pool (all in use)
├─ Wait for return (default 30s timeout)
├─ If timeout: DataSourceException thrown
├─ Gateway: 503 Service Unavailable
└─ ⚠ Prevent: limit concurrent requests (rate limiting)

SCENARIO 5: Redis Connection Loss
├─ Cache operations fail (non-critical)
├─ Bypass cache: fetch from database
├─ Performance degraded but service continues
└─ ✓ Graceful degradation

SCENARIO 6: RabbitMQ Connection Loss
├─ Event publishing fails immediately
├─ Service catches exception and handles
├─ Options:
│   a) Retry with exponential backoff
│   b) Store event locally, replay later (outbox pattern)
│   c) Fail fast and inform client
└─ ⚠ Depends on implementation

SCENARIO 7: Message Size Exceeds Message Broker Limit
├─ Chat Service tries to publish huge message
├─ RabbitMQ rejects (default max 128 MB)
├─ Service catches AMQPException
├─ Returns 413 Payload Too Large to client
└─ ✓ Graceful error response
```

### Idempotency Guarantees

```
Problem: Message sent twice due to network retry

Solution: Idempotent message handlers

@RabbitListener(queues = "chat.message.sent.queue")
public void onChatMessageSent(ChatMessagePayload payload) {
    // Check if message already persisted
    Optional<Message> existing = messageRepository
        .findById(payload.getMessageId());
    
    if (existing.isPresent()) {
        log.debug("Message already persisted: {}", payload.getMessageId());
        return; // Idempotent ✓
    }
    
    // Persist message (only if first time)
    Message message = new Message();
    message.setId(payload.getMessageId());
    // ... set other fields
    messageRepository.save(message);
}

Result: Can safely retry failed RabbitMQ messages
        No duplicate persistence
```

---

## Deployment Architecture

### Container Topology

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRODUCTION CLUSTER                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Load Balancer (Nginx / HAProxy)                                │
│  ├─ Handle TLS termination                                      │
│  ├─ Route to API Gateway instances                              │
│  └─ Health check every 5 seconds                                │
│       │                                                         │
│       ├─ API Gateway Instance 1 (8080)                          │
│       ├─ API Gateway Instance 2 (8080)                          │
│       └─ API Gateway Instance 3 (8080)                          │
│             │                                                   │
│             ├─── Auth Service Instance 1 (8081)                 │
│             ├─── Auth Service Instance 2 (8081)                 │
│             ├─ User Service Instance 1 (8082)                   │
│             ├─ User Service Instance 2 (8082)                   │
│             ├─ User Service Instance 3 (8082)                   │
│             ├─ Chat Service Instance 1 (8083) [sticky session]  │
│             ├─ Chat Service Instance 2 (8083) [sticky session]  │
│             ├─ Chat Service Instance 3 (8083) [sticky session]  │
│             ├─ Message Service Instance 1 (8084)                │
│             ├─ Message Service Instance 2 (8084)                │
│             ├─ Message Service Instance 3 (8084)                │
│             ├─ Notification Service Instance 1 (8085)           │
│             └─ Notification Service Instance 2 (8085)           │
│                     │                                           │
│  ┌────────────────┼──────────────┬──────────────┐               │
│  │                │              │              │               │
│  ▼                ▼              ▼              ▼               │
│  PostgreSQL      Redis           RabbitMQ      MinIO            │
│  Cluster         Cluster         Cluster       Cluster          │
│  (3 nodes)       (3 nodes)       (3 nodes)     (3 nodes)        │
│  - auth_db       - cache         - messages    - attachments    │
│  - user_db       - sessions      - exchanges   - thumbnails     │
│  - message_db    - blacklist     - queues                       │
│  - notification_ - ShedLock      - durability                   │
│    db                                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

Note:
- API Gateway: No sticky session (stateless)
- Chat Service: Sticky session (WebSocket state tied to instance)
  RabbitMQ relay allows messages to sync across instances
- All DBs: Replication for HA
- All services: Health checks, auto-restart on failure
```

### Kubernetes Deployment (Example)

```yaml
# Deployment for Auth Service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: orangchat/auth-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: AUTH_SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: auth-config
              key: datasource-url
        - name: REDIS_HOST
          value: redis-service
        - name: SPRING_RABBITMQ_HOST
          value: rabbitmq-service
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/ready
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            cpu: 250m
            memory: 512Mi
          limits:
            cpu: 500m
            memory: 1Gi

---
# Service to expose Auth Service
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  selector:
    app: auth-service
  ports:
  - protocol: TCP
    port: 8081
    targetPort: 8081
  type: ClusterIP
```

---

## Summary

The Orang Chat backend architecture provides:

✅ **Scalability**: Horizontal service scaling, database sharding, caching
✅ **Reliability**: Event-driven resilience, idempotent handlers, graceful degradation
✅ **Security**: JWT authentication, role-based authorization, encrypted storage
✅ **Maintainability**: Clear service boundaries, well-documented patterns
✅ **Observability**: Structured logging, health checks, metrics

The design supports growth from thousands to millions of concurrent users through:
- Microservices isolation
- Database per service
- Async event processing
- Distributed caching
- Connection pooling
- Rate limiting & circuit breakers

All components are designed for high availability with redundancy at every layer.
