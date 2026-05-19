# Orang Chat Backend 🍊

A modern, cloud-native microservices chat application built with **Java 21**, **Spring Boot 3.5**, and **Spring Cloud**. Designed for scalability, real-time communication, and enterprise-grade reliability.

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Services Overview](#services-overview)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Database Setup](#database-setup)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Orang Chat is a **production-ready backend** for a real-time messaging platform. It handles:

✅ **Authentication & Authorization** - JWT tokens, password reset, email verification
✅ **User Management** - Profiles, contacts, friend requests, blocking, presence tracking
✅ **Real-Time Messaging** - WebSocket/STOMP with horizontal scaling via RabbitMQ
✅ **Persistent Storage** - Messages, conversations, reactions, pins, read receipts
✅ **File Attachments** - MinIO S3-compatible storage with async thumbnail generation
✅ **Web Push Notifications** - Native browser notifications with VAPID protocol
✅ **Event-Driven Architecture** - Loose coupling via RabbitMQ message broker
✅ **Full-Text Search** - PostgreSQL GIN indexes for searching 1M+ messages

**Key Stats:**
- **7 microservices** deployed independently
- **4 PostgreSQL databases** (per-service ownership)
- **1 RabbitMQ broker** for event-driven async processing
- **Redis cache** for sessions, caching, and distributed locking
- **S3-compatible MinIO** for object storage
- **JWT + stateless auth** across all services
- **80%+ test coverage** on critical services

---

## 🛠 Tech Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 21 | Modern JVM with virtual threads |
| **Framework** | Spring Boot | 3.5.11 | Application framework |
| **Cloud** | Spring Cloud | 2025.0.1 | Microservices orchestration |
| **Gateway** | Spring Cloud Gateway | Latest | API routing & rate limiting |
| **Databases** | PostgreSQL | 15+ | Relational data storage |
| **Cache** | Redis | 7.0+ | Session cache, token blacklist |
| **Message Broker** | RabbitMQ | 4.0+ | Event-driven messaging |
| **Storage** | MinIO | 8.5.7 | S3-compatible object storage |
| **Security** | Spring Security | 6.x | Authentication & authorization |
| **ORM** | Hibernate | 6.x | JPA entity mapping |
| **Migrations** | Flyway | Latest | Database schema versioning |
| **Auth** | JJWT | 0.12.5 | JWT generation & validation |
| **Web Push** | web-push | 5.1.1 | VAPID protocol implementation |
| **Documentation** | SpringDoc OpenAPI | 2.8.16 | Swagger/OpenAPI 3.0 |
| **Testing** | JUnit 5 + Testcontainers | Latest | Unit & integration tests |
| **Build** | Maven | 3.9+ | Dependency management |

See [docs/TECHNOLOGY.md](docs/TECHNOLOGY.md) for detailed technology descriptions and design patterns.

---

## 📁 Project Structure

```
backend/
├── api-gateway/                    # Spring Cloud Gateway (8080)
├── auth-service/                   # Authentication & Authorization (8081)
├── user-service/                   # User Profiles & Contacts (8082)
├── chat-service/                   # WebSocket Real-Time Messaging (8083)
├── message-service/                # Message Persistence (8084)
├── notification-service/           # Push Notifications (8085)
├── shared-library/                 # Common Utilities (no port)
├── docs/                           # Documentation
│   ├── api/
│   │   ├── auth-api.md
│   │   └── technical-debt.md
│   ├── feature-status.md
│   ├── email-error-handling.md
│   ├── TECHNOLOGY.md               # Tech stack & design patterns
│   └── ARCHITECTURE.md             # System architecture & design
├── docker-compose.yml              # Infrastructure setup
├── service.Dockerfile              # Multi-stage build for services
├── pom.xml                         # Parent POM (module definitions)
└── README.md                       # This file
```

---

## 📋 Requirements

- **Java**: 21+
- **Maven**: 3.9+
- **Docker & Docker Compose**: For infrastructure services
- **Git**: For cloning the repository

---

## 🚀 Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/OrangCorp/orang-chat.git
cd orang-chat
```

### 2. Start Infrastructure

Start all supporting services:

```bash
cd backend
docker-compose up -d
```

**Verify services started:**
```bash
docker-compose ps
```

### 3. Build All Services

```bash
./mvnw clean install
```

### 4. Start Services

**Option A: Development (individual terminals):**

```bash
# Terminal 1 - Auth Service
cd auth-service && ../mvnw spring-boot:run

# Terminal 2 - User Service  
cd user-service && ../mvnw spring-boot:run

# Terminal 3 - Chat Service
cd chat-service && ../mvnw spring-boot:run

# Terminal 4 - Message Service
cd message-service && ../mvnw spring-boot:run

# Terminal 5 - Notification Service
cd notification-service && ../mvnw spring-boot:run

# Terminal 6 - API Gateway
cd api-gateway && ../mvnw spring-boot:run
```

**Option B: Production (Docker):**

```bash
docker-compose up -d
```

### 5. Test API

Register a user:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "displayName": "John Doe"
  }'
```

### 6. API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

---

## 🏗 Services Overview

| Service | Port | Role | Responsibility |
|---------|------|------|---|
| **API Gateway** | 8080 | Entry point | Routing, rate limiting, JWT validation |
| **Auth Service** | 8081 | Identity | Registration, login, JWT generation, email verification |
| **User Service** | 8082 | Profiles | User profiles, contacts, presence, blocking |
| **Chat Service** | 8083 | Real-time | WebSocket/STOMP messaging with RabbitMQ relay |
| **Message Service** | 8084 | Persistence | Message storage, conversations, reactions, search |
| **Notification Service** | 8085 | Push | Web Push subscriptions, notifications |
| **Shared Library** | — | Common | JWT utils, security filters, events, DTOs |

For detailed service information, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 📚 Documentation

- **[docs/TECHNOLOGY.md](docs/TECHNOLOGY.md)** - Technology stack, design patterns, solutions to core problems
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture, data flows, deployment topology
- **[docs/feature-status.md](docs/feature-status.md)** - Feature implementation status
- **[docs/api/auth-api.md](docs/api/auth-api.md)** - Authentication API details
- **[docs/api/technical-debt.md](docs/api/technical-debt.md)** - Known technical debt

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in `backend/`:

```bash
# Required database credentials
AUTH_DB_USER=authuser
AUTH_DB_PASS=authpass
USER_DB_USER=useruser
USER_DB_PASS=userpass
MESSAGE_DB_USER=messageuser
MESSAGE_DB_PASS=messagepass
NOTIF_DB_USER=notificationuser
NOTIF_DB_PASS=notificationpass

# Shared secret used by every service and the gateway
JWT_SECRET=replace-with-a-long-random-256-bit-secret

# Redis
REDIS_PASSWORD=replace-with-a-strong-redis-password

# RabbitMQ
RABBIT_USER=orangchat
RABBIT_PASS=replace-with-a-strong-rabbitmq-password

# Email / SMTP
MAIL_HOST=smtp.mailtrap.io
MAIL_USER=replace-with-your-smtp-username
MAIL_PASS=replace-with-your-smtp-password

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ROOT_USER=replace-with-minio-access-key
MINIO_ROOT_PASS=replace-with-minio-secret-key
MINIO_DOWNLOAD_MODE=backend
MINIO_EXTERNAL_ENDPOINT=http://localhost:9000

# Web Push VAPID
VAPID_PUB=BAMSANY2VXcACzimQfQ32znJ4hjyLblT0lRRZZMPryQzfQ-2T1lLJhax_antSYDGx8mYgDmPQLjynIUP7GpryCo
VAPID_PRIV=1kdAe_QZMqdJZ1oQ0BTXJlk-gugzN_TFmQTSDLwBhaw
VAPID_SUBJECT=mailto:admin@example.com

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Internal service URLs used by the gateway
AUTH_SERVICE_URL=http://auth-service:8081
USER_SERVICE_URL=http://user-service:8082
CHAT_SERVICE_URL=ws://chat-service:8083
MESSAGE_SERVICE_URL=http://message-service:8084
NOTIFICATION_SERVICE_URL=http://notification-service:8085
```

The Docker Compose stack passes the database URLs internally. If you run a service outside Compose, export the matching `*_SPRING_DATASOURCE_URL` value as well.

### Spring Boot Configuration

Each service has `application.yaml` in `src/main/resources/`. See individual service documentation.

---

## 🗄 Database Setup

Flyway automatically runs migrations on startup. To manually create databases:

```bash
# Create auth_db
docker exec orangchat-postgres-auth createdb auth_db -U postgres

# Create user_db  
docker exec orangchat-postgres-user createdb user_db -U postgres

# Create message_db
docker exec orangchat-postgres-message createdb message_db -U postgres

# Create notification_db
docker exec orangchat-postgres-notification createdb notification_db -U postgres
```

Connect to database:

```bash
docker exec -it orangchat-postgres-message psql -U messageuser -d message_db
```

---

## 🧪 Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Module Tests

```bash
cd message-service && ../../mvnw test
```

### Generate Coverage Report

```bash
./mvnw test -Pjacoco
open message-service/target/site/jacoco/index.html
```

**Current Coverage:**
- auth-service: ~75%
- user-service: ~70%
- message-service: ~80%
- notification-service: ~89%

---

## 🚢 Deployment

### Build Docker Images

```bash
docker-compose build
```

### Push to Registry

```bash
docker tag orangchat/auth-service:latest your-registry/auth-service:latest
docker push your-registry/auth-service:latest
```

### Deploy to Kubernetes

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#deployment-architecture) for Kubernetes YAML examples.

---

## 🔧 Troubleshooting

### Service won't start

Check Docker containers:
```bash
docker-compose ps
docker-compose logs auth-service
```

### Database connection errors

Verify environment variables:
```bash
echo $AUTH_SPRING_DATASOURCE_URL
```

### JWT token issues

Ensure JWT_SECRET is set and same across services:
```bash
echo $JWT_SECRET | wc -c  # Should be 256+ bits
```

### RabbitMQ not receiving messages

Check RabbitMQ console: http://localhost:15672

### WebSocket connection fails

Verify Chat Service is running:
```bash
curl http://localhost:8083/actuator/health
```

See [README.md troubleshooting section](#troubleshooting) for more details.

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Add tests
4. Run tests: `./mvnw test`
5. Commit: `git commit -m 'Add amazing feature'`
6. Push: `git push origin feature/amazing-feature`
7. Open Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

## 🔗 Quick Links

- **Issues**: https://github.com/OrangCorp/orang-chat/issues
- **Discussions**: https://github.com/OrangCorp/orang-chat/discussions
- **Frontend Repo**: https://github.com/OrangCorp/orang-chat-frontend

---

Made with 🍊 by the Orang Chat team
