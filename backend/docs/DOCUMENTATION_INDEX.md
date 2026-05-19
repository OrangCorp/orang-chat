# Backend Documentation Index

This document provides a comprehensive overview of all Orang Chat backend documentation.

---

## 📖 Main Documentation Files

### 1. **README.md** (START HERE)
- Quick start guide (clone, setup, run)
- Overview of all 7 microservices
- Technology stack summary
- Quick troubleshooting section
- Configuration basics
- **Purpose**: Entry point for new developers

### 2. **[docs/TECHNOLOGY.md](TECHNOLOGY.md)** (DETAILED TECH STACK)
- Complete technology stack with versions
- Architecture overview diagram
- **10 Core Solutions** implemented:
  1. Event-Driven Architecture with RabbitMQ
  2. JWT Authentication with Token Blacklisting
  3. Per-Service PostgreSQL Databases
  4. Real-Time WebSocket with External RabbitMQ Relay
  5. Web Push Notifications with VAPID Protocol
  6. Async Attachment Processing with Thumbnails
  7. Full-Text Search on Messages
  8. Rate Limiting per Service
  9. Contact Request System with Blocking
  10. Distributed Task Scheduling with ShedLock
- Infrastructure setup (Docker Compose)
- Database migrations (Flyway)
- Security architecture
- Performance & scalability tuning
- **Purpose**: Understand WHY we chose these technologies and HOW they solve problems

### 3. **[docs/ARCHITECTURE.md](ARCHITECTURE.md)** (SYSTEM DESIGN)
- System architecture diagram with all components
- **10 Design Patterns** used:
  1. Microservices Pattern
  2. API Gateway Pattern
  3. Event-Driven Pattern
  4. Saga Pattern (Distributed Transactions)
  5. Repository Pattern
  6. Service Layer Pattern
  7. JWT (Stateless) Authentication
  8. Circuit Breaker Pattern (ready for implementation)
  9. Bulkhead Pattern (ready for implementation)
  10. Retry Pattern
- Detailed **Data Flows** for:
  - User Registration Flow
  - Message Send Flow (WebSocket)
- API Gateway design & routing
- Service layer architecture (pattern)
- Database design with ERD
- Message queue architecture (RabbitMQ)
- Security architecture (JWT token flow)
- Resilience & fault tolerance (failure scenarios)
- Deployment architecture (Kubernetes examples)
- **Purpose**: Understand HOW the system works and how components interact

---

## 📚 Existing Documentation Files

Located in `docs/` directory:

### 4. **docs/feature-status.md**
- Complete list of implemented features by service
- Authentication features (register, login, logout, refresh, email verification)
- User management features (profiles, contacts, search, presence)
- Messaging features (conversations, messages, reactions, pins, attachments)
- Chat service features (STOMP, presence tracking)
- Event-driven features (profile creation from events)
- **Purpose**: Track feature completion status

### 5. **docs/api/auth-api.md**
- Auth Service API endpoint documentation
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/refresh
- POST /api/auth/logout
- Detailed request/response examples
- Error handling
- **Purpose**: Auth API specification

### 6. **docs/api/technical-debt.md**
- Known technical debt items
- Profile data ownership split
- Silent JWT filter failures
- WebSocket security exception semantics
- WebSocket CORS wide open
- Recommendations for each item
- **Purpose**: Track improvements needed

### 7. **docs/email-error-handling.md**
- Email service error handling architecture
- EmailServiceException class
- Enhanced EmailService with error recovery
- GlobalExceptionHandler integration
- Error types and handling strategies
- **Purpose**: Email service reliability

---

## 🔍 Documentation by Audience

### For Backend Developers (First Time Here)
1. Read: [../../README.md](../../README.md) - Quick start
2. Read: [TECHNOLOGY.md](TECHNOLOGY.md) - Tech stack overview
3. Skim: [ARCHITECTURE.md](ARCHITECTURE.md) - System design
4. Explore: [feature-status.md](feature-status.md) - What's implemented

### For Architects & Tech Leads
1. Read: [ARCHITECTURE.md](ARCHITECTURE.md) - Complete system design
2. Read: [TECHNOLOGY.md](TECHNOLOGY.md) - Design patterns & solutions
3. Review: [api/technical-debt.md](api/technical-debt.md) - Improvement areas
4. Reference: [feature-status.md](feature-status.md) - Feature roadmap

### For DevOps & Operations
1. Read: [../../README.md](../../README.md#deployment) - Deployment section
2. Read: [ARCHITECTURE.md](ARCHITECTURE.md#deployment-architecture) - Deployment topology
3. Review: [../../README.md](../../README.md#configuration) - Configuration
4. Reference: docker-compose.yml - Infrastructure setup

### For QA & Testing
1. Read: [../../README.md](../../README.md#testing) - Testing section
2. Reference: [feature-status.md](feature-status.md) - Features to test
3. Review: [ARCHITECTURE.md](ARCHITECTURE.md#resilience--fault-tolerance) - Failure scenarios
4. Check: [api/](api/) files for API specifications
5. Coverage snapshot: 77.38% line / 75.99% instruction / 59.73% branch / 78.21% method

### For Frontend Developers
1. Read: [../../README.md](../../README.md#api-documentation) - API docs link
2. Read: [api/auth-api.md](api/auth-api.md) - Auth API details
3. Reference: [feature-status.md](feature-status.md#14-message-service) - Message endpoints
4. Check: Swagger UI at http://localhost:8080/swagger-ui.html (when running)

---

## 🏗 Architecture at a Glance

```
┌─────────────────────────────────────────────────────────┐
│                   API Gateway (8080)                    │
│         Route requests, validate JWT, rate limit        │
└────────────┬───────────────────────────────────────────┘
             │
    ┌────────┼────────┬────────┬──────────┐
    ▼        ▼        ▼        ▼          ▼
┌────────┐ ┌────────┐ ┌──────┐ ┌────────┐ ┌────────────┐
│Auth    │ │User    │ │Chat  │ │Message │ │Notification
│Service │ │Service │ │Svc   │ │Service │ │Service
│(8081)  │ │(8082)  │ │(8083)│ │(8084)  │ │(8085)
└────────┘ └────────┘ └──────┘ └────────┘ └────────────┘
     │         │         │         │          │
     └─────────┼────┬────┼────┬────┼──────────┘
               │    │    │    │    │
         ┌─────▼────▼──┐ ┌──▼───▼──────┐
         │  PostgreSQL │ │   RabbitMQ   │
         │  (4 x DBs)  │ │   (Events)   │
         └─────────────┘ └──────────────┘
                └─────────┬──────────┐
                     ┌────▼─────┐   │
                     │  Redis    │   │
                     │(Cache)    │   │
                     └───────────┘   │
                              ┌──────▼──────┐
                              │   MinIO      │
                              │ (Storage)    │
                              └──────────────┘
```

---

## 📊 Technology Comparison

### Databases (Per-Service)
- **Auth DB**: User credentials, email verification
- **User DB**: Profiles, contacts, blocking
- **Message DB**: Messages, conversations, reactions, pins
- **Notification DB**: Push subscriptions, preferences

### Messaging (RabbitMQ)
- **user.exchange** → Profile creation from registration
- **contact.exchange** → Contact request notifications
- **chat.exchange** → Real-time message relay
- **message.exchange** → Message mutations (edit, delete, react)
- **attachment.exchange** → Thumbnail generation
- **notification.exchange** → Push delivery

### Caching (Redis)
- **Sessions**: User session data
- **Token Blacklist**: Logout management
- **Cache**: Profile, contact lists
- **ShedLock**: Distributed task locking

### Storage (MinIO)
- **Attachments**: File uploads (S3-compatible)
- **Thumbnails**: Generated async from images

---

## 🔐 Security Architecture

### Authentication
- **JWT**: HMAC-SHA256 with 256-bit secret
- **Access Token**: 15 minutes TTL
- **Refresh Token**: 7 days TTL
- **Reuse Detection**: Full session blacklist on reuse

### Authorization
- **Role-Based**: Group members, admin roles
- **Ownership**: User owns their data
- **Relation Check**: Can only message accepted contacts

### Password Security
- **Hashing**: BCrypt (Spring Security default)
- **Email Verification**: 6-digit code (15-min TTL)
- **Reset**: Temporary link via email

---

## 🚀 Performance Characteristics

### Scalability
- **Horizontal**: Add service instances behind load balancer
- **Database**: Connection pooling (HikariCP), per-service scaling
- **Messaging**: RabbitMQ allows decoupled processing
- **Cache**: Redis for frequently accessed data

### Response Times (Expected)
- User login: < 200ms
- Message send: < 100ms (WebSocket async)
- Search (1M messages): 10-50ms (GIN index)
- Rate limit check: < 5ms (Redis)

### Concurrency
- **Virtual Threads** (Java 21): 10K+ concurrent requests per instance
- **WebSocket**: RabbitMQ relay enables 100K+ concurrent users across cluster
- **Database**: HikariCP connection pool (default 10 connections)

---

## 🛠 Development Workflow

### Local Development
```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Build everything
./mvnw clean install

# 3. Run each service in separate terminal
cd {service} && ../../mvnw spring-boot:run

# 4. Test
curl http://localhost:8080/api/auth/register ...
```

### Testing
```bash
# Run all tests
./mvnw test

# Run specific service tests
cd message-service && ../../mvnw test

# Generate coverage
./mvnw test -Pjacoco
```

### Deployment
```bash
# Build Docker images
docker-compose build

# Push to registry
docker push your-registry/service:latest

# Deploy to Kubernetes
kubectl apply -f k8s/
```

---

## 📝 Common Tasks

### Add New Microservice
1. Create new module with same structure as existing service
2. Add to parent pom.xml `<modules>`
3. Implement controller, service, repository layers
4. Add database migrations (Flyway)
5. Add RabbitMQ listeners if consuming events
6. Add route to API Gateway configuration
7. Document in feature-status.md

### Add New Event
1. Create event class in shared-library/event/
2. Add publisher in source service
3. Add listener in consuming service
4. Add RabbitMQ queue/exchange config
5. Document in ARCHITECTURE.md

### Add New API Endpoint
1. Create controller method
2. Add @Operation annotation for OpenAPI
3. Add authorization check
4. Add request/response DTOs
5. Add tests (unit + integration)
6. Swagger UI auto-updates

---

## 📞 Support & Questions

- **Issues**: https://github.com/OrangCorp/orang-chat/issues
- **Discussions**: https://github.com/OrangCorp/orang-chat/discussions
- **Slack**: #orang-chat-dev (internal)
- **Docs**: See files in backend/docs/

---

## 📈 What's Next

- Implement circuit breakers (Resilience4j)
- Add distributed tracing (Sleuth + Jaeger)
- Implement API versioning
- Add comprehensive audit logging
- Implement end-to-end encryption
- Add 2FA support
- Implement database read replicas
- Add WebSocket compression
- Implement GraphQL API

See [api/technical-debt.md](api/technical-debt.md) for more details.

---

## 📄 File Organization

```
backend/
├── README.md                    ← START HERE
├── docker-compose.yml
├── pom.xml
├── docs/
│   ├── README.md               (if created)
│   ├── TECHNOLOGY.md           ← Tech stack & solutions
│   ├── ARCHITECTURE.md         ← System design & patterns
│   ├── DOCUMENTATION_INDEX.md  ← This file
│   ├── feature-status.md       ← Feature checklist
│   ├── email-error-handling.md
│   └── api/
│       ├── auth-api.md
│       └── technical-debt.md
└── {services}/                 ← Source code
```

---

Last Updated: May 10, 2026
Documentation Version: 1.0 (Comprehensive)
