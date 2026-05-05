# Orang Chat Backend - Technology Stack & Solutions

## Table of Contents
1. [Technology Stack](#technology-stack)
2. [Architecture Overview](#architecture-overview)
3. [Core Solutions](#core-solutions)
4. [Infrastructure](#infrastructure)
5. [Security](#security)
6. [Performance & Scalability](#performance--scalability)

---

## Technology Stack

### Language & Frameworks
| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 | Modern language with virtual threads for scalability |
| **Spring Boot** | 3.5.11 | Application framework |
| **Spring Cloud** | 2025.0.1 | Microservices orchestration & cloud patterns |
| **Spring Cloud Gateway** | Latest | API Gateway for routing & load balancing |
| **Spring Security** | 6.x | Authentication & authorization framework |
| **Spring Data JPA** | Latest | ORM abstraction layer |
| **Spring Data Redis** | Latest | Cache & session management |
| **Spring AMQP** | Latest | RabbitMQ integration for event-driven messaging |
| **Spring WebSocket** | Latest | Real-time communication via STOMP |

### Cryptography & Security
| Library | Version | Purpose |
|---------|---------|---------|
| **JJWT (JWT)** | 0.12.5 | JWT token generation & validation with HMAC-SHA256 |
| **Spring Security Crypto** | Latest | BCrypt password hashing |
| **Bouncy Castle** | Latest | Cryptography provider for Web Push (VAPID) |
| **web-push** | 5.1.1 | Web Push Protocol implementation |

### Persistence & Data Access
| Technology | Version | Purpose |
|------------|---------|---------|
| **PostgreSQL** | 15+ | Primary relational database (per-service instances) |
| **Hibernate** | 6.x | JPA entity mapping & ORM |
| **Spring Data JPA** | Latest | Repository pattern implementation |
| **Flyway** | Latest | Database schema versioning & migrations |
| **JdbcTemplate** | Latest | Raw SQL execution when needed |

### Caching & Session Management
| Technology | Version | Purpose |
|------------|---------|---------|
| **Redis** | 7.0+ | Distributed cache, session store, token blacklist |
| **Lettuce** | Latest | Redis Java client (async, non-blocking) |
| **Spring Data Redis** | Latest | Redis integration layer |
| **ShedLock** | 6.3.1 | Distributed task locking for scheduled jobs |

### Message Broker
| Component | Version | Purpose |
|-----------|---------|---------|
| **RabbitMQ** | 4.0+ | Event-driven messaging broker |
| **Spring AMQP** | Latest | RabbitMQ abstraction |
| **Jackson2JsonMessageConverter** | Latest | JSON serialization for messages |

### Object Storage
| Technology | Version | Purpose |
|------------|---------|---------|
| **MinIO** | 8.5.7 | S3-compatible object storage for attachments |
| **MinIO Java Client** | 8.5.7 | MinIO SDK integration |
| **Thumbnailator** | 0.4.20 | Image thumbnail generation |
| **Spring Retry** | Latest | Retry logic for async thumbnail generation |

### Documentation & API
| Technology | Version | Purpose |
|------------|---------|---------|
| **SpringDoc OpenAPI** | 2.8.16 | OpenAPI 3.0 / Swagger UI generation |
| **Swagger UI** | Latest | Interactive API documentation |
| **Springdoc Annotations** | Latest | @Operation, @Tag for endpoint documentation |

### Testing
| Framework | Version | Purpose |
|-----------|---------|---------|
| **JUnit 5 (Jupiter)** | 5.10+ | Unit testing framework |
| **Mockito** | 5.x | Mocking framework for unit tests |
| **Spring Test** | Latest | Spring Boot test utilities |
| **Spring Security Test** | Latest | Security-context mocking for protected endpoints |
| **Testcontainers** | Latest | Docker containers for PostgreSQL/RabbitMQ in tests |
| **AssertJ** | Latest | Fluent assertions for readable test code |
| **H2 Database** | Latest | In-memory DB for lightweight tests |
| **JaCoCo** | 0.8.12 | Code coverage measurement |

### Build & Dependency Management
| Tool | Version | Purpose |
|------|---------|---------|
| **Maven** | 3.9+ | Build automation & dependency management |
| **Maven Wrapper** | Included | Consistent Maven version across environments |
| **Lombok** | 1.18.30 | Reduce boilerplate with annotations |

### Monitoring & Logging
| Component | Purpose |
|-----------|---------|
| **Spring Boot Actuator** | Health checks, metrics endpoints |
| **Spring Cloud Sleuth** | Distributed tracing (prepared for integration) |
| **SLF4J + Logback** | Structured logging |

---

## Architecture Overview

### Microservices Topology

```
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                           │
│              Spring Cloud Gateway + Rate Limiting               │
└──┬──────────┬──────────┬──────────┬──────────┬──────────────────┘
   │          │          │          │          │
   ├─ Auth    ├─ User    ├─ Chat    ├─ Message ├─ Notification
   │ (8081)   │ (8082)   │ (8083)   │ (8084)   │ (8085)
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐ ┌──────┐ ┌─────────┐ ┌──────┐ ┌──────────┐
│Auth  │ │User  │ │  Chat   │ │Msg   │ │Notif    │
│DB    │ │DB    │ │WebSocket│ │DB    │ │DB       │
│      │ │      │ └─────────┘ │      │ │         │
│ POST │ │ POST │             │ POST │ │ POST    │
│(5432)│ │(5433)│    Redis    │(5434)│ │(5435)   │
└──────┘ └──────┘  Lettuce    └──────┘ └──────────┘
   │        │       (6379)         │        │
   │        │                      │        │
   └────────┼──────────────────────┼────────┘
            │                      │
            ▼                      ▼
        ┌─────────────────────────────┐
        │    RabbitMQ Message Broker  │
        │   (Topic Exchanges + Queues)│
        │         (5672)              │
        └─────────────────────────────┘
            │
            ▼
        ┌──────────────┐
        │   MinIO S3   │
        │   Storage    │
        │   (9000)     │
        └──────────────┘

Shared Library:
├─ JWT Utilities
├─ Security Filters
├─ Event Models
├─ Constants & DTOs
└─ Common Exceptions
```

### Service Responsibilities

| Service | Responsibilities |
|---------|------------------|
| **API Gateway** | Route requests, rate limit, aggregate OpenAPI schemas, CORS handling |
| **Auth Service** | User registration, login, JWT token generation, email verification, password reset |
| **User Service** | User profiles, contact management, friend requests, blocking, presence tracking |
| **Chat Service** | WebSocket/STOMP real-time messaging, status tracking, external relay to RabbitMQ |
| **Message Service** | Persistent message storage, conversations (direct/group), reactions, pins, search |
| **Notification Service** | Web Push subscriptions, notification preferences, push delivery via RabbitMQ events |
| **Shared Library** | Common utilities, JWT helpers, security filters, event models, constants |

---

## Core Solutions

### 1. **Event-Driven Architecture with RabbitMQ**

**Problem Solved:** Tight coupling between services; synchronous failures cascade.

**Solution:**
- **Topic Exchange Pattern**: Services publish events to topic exchanges
- **Durable Queues**: Guaranteed delivery even if consumer temporarily down
- **Jackson2JsonMessageConverter**: Type-safe event serialization

**Event Flow Examples:**

```
User Registration Flow:
  1. Auth Service registers user → saves to DB → commits
  2. Publishes UserRegisteredEvent to "user.exchange"
  3. User Service listens on "user.registered.queue"
  4. User Service creates profile idempotently
  5. If User Service is down, message waits in queue

Message Sent Flow:
  1. Chat Service receives WebSocket message
  2. Publishes ChatMessagePayload to "chat.exchange"
  3. Message Service listens & persists to DB
  4. Message Service publishes MessageSentEvent
  5. Notification Service listens & sends Web Push
  6. All services work independently; no cascade failures
```

**Benefits:**
- Service A doesn't know about Service B (loose coupling)
- Failures are isolated
- Easy to add new event listeners (scalability)
- Audit trail via event log

---

### 2. **JWT Authentication with Token Blacklisting**

**Problem Solved:** Stateless auth across microservices; preventing token reuse after logout.

**Solution:**

```
Registration/Login Flow:
  1. User sends credentials
  2. Auth Service validates → generates tokens
  3. Access Token (15 min TTL): Used for API calls
  4. Refresh Token (7 day TTL): Used to get new access token

Token Structure (JWT with HMAC-SHA256):
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "iat": 1714300800,
  "exp": 1714301700
}

Logout Flow:
  1. User sends access token + logout request
  2. Auth Service extracts userId from token
  3. Stores token in Redis blacklist with TTL = refresh expiration
  4. Any subsequent use of token is rejected

Token Refresh & Reuse Detection:
  1. Client sends refresh token
  2. Auth Service checks if token already used (Redis marker)
  3. If reused: ENTIRE user session blacklisted (security breach!)
  4. If fresh: New tokens issued, old refresh token marked as used
```

**Security Levels:**
- **Access Token**: 15 minutes (short window)
- **Refresh Token**: 7 days (longer; tied to device)
- **Blacklist**: Redis stores `user:{userId}:blacklist` key

**Benefits:**
- Stateless: No session replication needed
- Works across all services
- Token reuse triggers full session revocation
- Prevents compromised token reuse

---

### 3. **Per-Service PostgreSQL Databases**

**Problem Solved:** Data ownership; scaling independent services; avoiding single database bottleneck.

**Solution:**

Each service owns its database schema:
```
Auth Service owns:     auth_db (users credentials)
User Service owns:     user_db (profiles, contacts)
Message Service owns:  message_db (messages, conversations, attachments)
Notification Service:  notification_db (push subscriptions, preferences)
```

**Why Per-Service DB?**
- **Data Ownership**: User Service can't accidentally access Message Service tables
- **Schema Evolution**: Auth Service schema changes don't block Message Service deploys
- **Scaling**: Each database can be sized independently
- **Disaster Recovery**: Lost Message DB doesn't impact Auth DB

**Trade-offs:**
- No transactions across services (eventual consistency via events)
- Data sync via RabbitMQ events (not real-time)
- Requires thinking about distributed data

**Data Consistency Pattern:**
```
User updates contact → publishes ContactUpdatedEvent
Notification Service listens & eventually updates its cache
If Notification Service temporarily down, message waits in queue
When Service restarts, queue is replayed (eventual consistency ✓)
```

---

### 4. **Real-Time WebSocket with External RabbitMQ Relay**

**Problem Solved:** Scale WebSocket connections while keeping chat service stateless.

**Solution:**

```
Traditional WebSocket (single instance):
  Client A ──┐
  Client B ──┼─ WebSocket Server ─ Limited to single machine
  Client C ──┤
  
Scaled WebSocket with RabbitMQ Relay:
  Client A ──┐         ┌─ Instance 1 ──┐
             ├──────────┤   (1000 conn) ├──┐
  Client B ──┤         └────────────────┘  │
             │                              ├─ RabbitMQ ─ Broadcast
  Client C ──┤         ┌─ Instance 2 ──┐  │
             ├──────────┤   (1000 conn) ├──┘
  Client D ──┘         └────────────────┘
  
  Each instance can scale independently!
```

**Implementation:**
1. Client connects to Chat Service (Round-robin via Gateway)
2. Client sends message via STOMP `/app/chat.send`
3. Chat Service publishes to RabbitMQ `chat.exchange`
4. Chat Service ALSO broadcasts to local STOMP `/topic/conversation/{convId}`
5. Message Service listens & persists
6. All Chat Service instances receive message via RabbitMQ
7. All connected clients receive message via their local STOMP connection

**Benefits:**
- Horizontal scaling (add more Chat Service instances)
- Message persistence in Message Service
- No dependency on single chat instance
- Clients reconnect to any instance (via Gateway)

---

### 5. **Web Push Notifications with VAPID Protocol**

**Problem Solved:** Native browser notifications without proprietary services.

**Solution:**

```
VAPID (Voluntary Application Server Identification):
- Server generates ECDP256 key pair
- Server sends public key to client
- Client uses public key to validate server is legitimate
- No third-party notification service needed!

Flow:
1. Client browser generates push subscription
2. Client sends subscription endpoint + encryption keys to server
3. Server stores in NotificationService DB
4. When event occurs, server sends encrypted payload to subscription endpoint
5. Browser receives & shows native notification
6. User can click notification to return to app
```

**Data Model:**
```
push_subscriptions:
  - endpoint (unique): https://fcm.googleapis.com/fcm/send/...
  - p256dhKey (encryption key)
  - authKey (authentication)
  - userAgent (for tracking which device)
  - expiresAt (may be empty; some are permanent)
```

**Security:**
- ECDP256 asymmetric encryption (browser validates server)
- Payload encrypted in transit
- Server stores private key securely (environment variable)

**Benefits:**
- No vendor lock-in (uses standard Web Push Protocol)
- Works on all browsers with push support
- User controls notifications (browser settings)
- Native OS notifications (sounds, badges, actions)

---

### 6. **Async Attachment Processing with Thumbnails**

**Problem Solved:** Large files block message send; need async thumbnail generation.

**Solution:**

```
Message Send Flow:
1. Client uploads file to MinIO (returns fileId)
2. Client sends message with attachmentIds
3. Message Service persists message immediately (doesn't wait for thumbnails)
4. Message Service emits ThumbnailRequestedEvent to RabbitMQ
5. Returns response to client immediately ✓

Thumbnail Generation (async in background):
1. Notification Service listens to attachment.thumbnail.queue
2. Downloads original image from MinIO
3. Generates thumbnail with Thumbnailator
4. Uploads thumbnail to MinIO (separate key)
5. Emits ThumbnailReadyEvent to RabbitMQ
6. Message Service updates message with thumbnail URL
7. Sends WebSocket notification to all conversation participants
8. Frontend displays thumbnail (THUMBNAIL_READY message type)
```

**Benefits:**
- Message send is fast (no thumbnail waiting)
- Thumbnail generation doesn't block API thread
- If thumbnail fails, message still exists (graceful degradation)
- Can retry failed thumbnail generations later

**Data Model:**
```
attachments:
  - id (UUID)
  - messageId (FK)
  - fileType (image/jpeg, etc.)
  - fileSize (bytes)
  - s3Key (path in MinIO: "uploads/abc123.jpg")
  - thumbnailKey (nullable: "thumbnails/abc123.jpg")
  - thumbnailStatus (PENDING/READY/FAILED)
```

---

### 7. **Full-Text Search on Messages**

**Problem Solved:** Searching 1M+ messages with LIKE is slow.

**Solution:**

```
Database Setup:
  CREATE INDEX idx_messages_content_gin ON messages 
    USING GIN(to_tsvector('english', content));

Query Execution:
  SELECT * FROM messages 
  WHERE to_tsvector('english', content) @@ plainto_tsquery('english', 'hello')
  ORDER BY ts_rank(...) DESC
  LIMIT 20;
```

**Features:**
- English stemming (hello, hells, hello's → same root "hello")
- Ranking by relevance (ts_rank)
- Pagination support
- Optional within-conversation scoping

**Performance:**
- GIN index: ~1-10ms for 1M rows
- LIKE queries: 100-1000ms for 1M rows
- **100x faster** with full-text search ✓

---

### 8. **Rate Limiting per Service**

**Problem Solved:** DDoS; prevent single user from consuming all resources.

**Solution:**

```
Rate Limiting Rules (Gateway level):

Auth Service:     5 req/sec,  burst 10   (by IP address)
User Service:     20 req/sec, burst 40   (by user ID)
Message Service:  50 req/sec, burst 100  (by user ID)
Chat Service:     No limit (WebSocket)

Implementation:
1. Gateway extracts user ID from JWT (or IP if unauthenticated)
2. Creates Redis key: "ratelimit:{service}:{user_id}:{second}"
3. Increments counter
4. If counter > limit: return 429 Too Many Requests
5. Burst allows temporary spike (10 requests in sub-second)
```

**Benefits:**
- Prevents runaway clients
- Protects backend from overload
- Per-user tracking (fair share)
- Different limits per service (optimize each)

---

### 9. **Contact Request System with Blocking**

**Problem Solved:** Managing friend requests; preventing unwanted messages.

**Solution:**

```
Contact States:
  PENDING   → Requester sent, Recipient not responded
  ACCEPTED  → Both users can message each other
  BLOCKED   → Blocker prevents Blockee from messaging
  REJECTED  → Request declined (not stored long-term)

Workflow:
  User A → Contact Request → User B (status=PENDING)
  
  If User B accepts:
    Contact.status = ACCEPTED ✓ (User B can see in contacts)
    Contact.status = ACCEPTED ✓ (User A can see in contacts)
  
  If User B blocks:
    Contact.status = BLOCKED ✓ (User B blocked User A)
    User A cannot send messages to User B (blocked check in Message Service)
    User A cannot see User B in search results
    User A gets 403 Forbidden if trying to create conversation

Authorization Check in Message Service:
  1. User A sends message to User B
  2. Query: SELECT * FROM contacts WHERE 
       (requester_id = A AND recipient_id = B AND status != BLOCKED) OR
       (requester_id = B AND recipient_id = A AND status = ACCEPTED)
  3. If no match: return 403 Forbidden
```

**Data Model:**
```
contacts:
  - id
  - requester_id (who sent request)
  - recipient_id (who received request)
  - status (PENDING/ACCEPTED/BLOCKED)
  - acceptedAt (timestamp when accepted, null if pending)
  - createdAt, updatedAt
```

**Benefits:**
- Clear request workflow
- Bidirectional friendship (mutual acceptance)
- Blocking is unilateral (doesn't require other's consent)
- Audit trail via timestamps

---

### 10. **Distributed Task Scheduling with ShedLock**

**Problem Solved:** Scheduled jobs run on all services in cluster; duplicate work.

**Solution:**

```
Problem (without ShedLock):
  Cluster has 3 Auth Service instances
  All instances run "clean unverified users" job at midnight
  User deleted 3 times (inefficient!)

Solution with ShedLock:
  1. Only ONE instance acquires lock at a time
  2. Lock stored in Redis
  3. Other instances wait/skip
  4. If instance dies, lock expires → another acquires it

Implementation:
  @Scheduled(cron = "0 0 * * * *")  // Every hour
  @SchedulerLock(name = "cleanUnverifiedUsers", lockAtMostFor = "50m", lockAtLeastFor = "5m")
  public void cleanUnverifiedUsers() {
    // Runs only on one instance in cluster
  }
```

**Benefits:**
- No duplicate work in cluster
- Graceful failover (if instance dies, another picks up)
- Configurable lock duration (prevent concurrent runs)
- Redis-backed (shared across services)

---

## Infrastructure

### Docker Compose Stack

```yaml
Services:
  - PostgreSQL (Auth): auth-db on :5432
  - PostgreSQL (User): user-db on :5433
  - PostgreSQL (Message): message-db on :5434
  - PostgreSQL (Notification): notification-db on :5435
  - Redis: :6379 (cache, sessions, ShedLock)
  - RabbitMQ: :5672, Management UI :15672
  - MinIO: :9000 (Object Storage)
  - Spring Applications: 8080-8085
```

### Database Migrations (Flyway)

Each service includes versioned migrations:
```
auth-service/src/main/resources/db/migration/
  ├─ V1__init.sql                    (Initial schema)
  ├─ V2__email_verified.sql          (Add emailVerified column)
  └─ V3__normalize_case.sql          (Normalize email casing)

message-service/src/main/resources/db/migration/
  ├─ V1__init.sql                    (Messages, conversations, participants)
  ├─ V2__full_text_search.sql        (GIN indexes for search)
  ├─ V3__roles.sql                   (MEMBER/ADMIN roles)
  ├─ V4__features.sql                (Reactions, pins, reads)
  └─ ...
```

### Deployment

```bash
# Start infrastructure
docker-compose up -d

# Build all services
./mvnw clean install

# Start services (can be containerized)
cd api-gateway && ../mvnw spring-boot:run &
cd auth-service && ../mvnw spring-boot:run &
cd user-service && ../mvnw spring-boot:run &
cd chat-service && ../mvnw spring-boot:run &
cd message-service && ../mvnw spring-boot:run &
cd notification-service && ../mvnw spring-boot:run &
```

---

## Security

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

---

## Performance & Scalability

### Horizontal Scaling
- **Stateless Services**: Can run multiple instances behind Gateway
- **Redis Caching**: Shared cache layer
- **RabbitMQ Message Broker**: Decouples services
- **Database Connection Pooling**: HikariCP (default in Spring Boot)

### Vertical Scaling
- **Java 21 Virtual Threads**: More concurrent requests per instance
- **Async Processing**: RabbitMQ, Spring Async, WebSocket
- **Database Indexing**: GIN indexes for full-text search

### Caching Strategy
- **User Profiles**: Cached in Redis (TTL: 1 hour)
- **Contact List**: Cached per user (TTL: 30 min)
- **Session Data**: Redis-backed (auto-expired)
- **Token Blacklist**: Redis (expiry = refresh token TTL)

### Database Optimization
- **Connection Pooling**: HikariCP (default 10 connections)
- **Lazy Loading**: Prevent N+1 queries
- **Query Optimization**: Use SELECT * only when needed
- **Indexing Strategy**:
  - Primary keys: UUID indexed by default
  - Foreign keys: Indexed for JOINs
  - Search: GIN indexes on message content
  - User lookup: Indexed on email (unique)

### Monitoring & Observability
- **Spring Boot Actuator**: `/actuator/health`, `/actuator/metrics`
- **Database Metrics**: Connection pool, query times
- **RabbitMQ Metrics**: Queue depth, message throughput
- **Business Metrics**: Messages sent/received, user registrations

---

## Summary

The Orang Chat backend is designed for **scalability, reliability, and maintainability**:

✅ **Microservices**: Independent services, own databases, clear responsibilities
✅ **Event-Driven**: Loose coupling, graceful degradation, easy to extend
✅ **Real-Time**: WebSocket + RabbitMQ relay for truly global chat
✅ **Security**: JWT + blacklisting, role-based authorization, input validation
✅ **Performance**: Caching, indexing, async processing, connection pooling
✅ **Testing**: JUnit 5, Testcontainers, 80%+ coverage in critical services
✅ **Operations**: Docker, Flyway migrations, health checks, metrics

The architecture supports millions of users with proper resource management and horizontal scaling.
