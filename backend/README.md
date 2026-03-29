# Orang 🍊

Orang Chat is a modern, cloud-native microservices chat application built with Spring Boot and Spring Cloud. It provides a scalable architecture for real-time communication, user management, and authentication.

## 🚀 Features

- **Microservices Architecture**: Independently deployable services for authentication, user management, and messaging.
- **API Gateway**: Single entry point for all client requests using Spring Cloud Gateway.
- **Authentication & Authorization**: Secure access using JWT (JSON Web Tokens).
- **Real-time Communication**: WebSocket/STOMP support for instant messaging.
- **Service Communication**: Event-driven architecture with RabbitMQ for asynchronous processing.
- **Object Storage**: Profile pictures and attachments storage with MinIO (planned).
- **Persistence**: Per-service PostgreSQL databases for data isolation.

See [docs/feature-status.md](docs/feature-status.md) for a full breakdown of implemented, partially implemented, and planned features.

## 🛠 Tech Stack

- **Language**: Java 21
- **Frameworks**: Spring Boot 3.5.x, Spring Cloud 2025.x
- **Build Tool**: Maven
- **Databases**: PostgreSQL, Redis (for caching/sessions)
- **Messaging**: RabbitMQ
- **Object Storage**: MinIO
- **Security**: Spring Security, JWT
- **Documentation**: SpringDoc OpenAPI (Swagger)

## 📁 Project Structure

```text
orang-chat/
├── api-gateway/       # Spring Cloud Gateway (Port: 8080)
├── auth-service/      # Authentication & Authorization (Port: 8081)
├── user-service/      # User Profiles & Contacts (Port: 8082)
├── chat-service/      # WebSocket Real-time Messaging (Port: 8083)
├── message-service/   # Message Persistence & Conversations (Port: 8084)
├── shared-library/    # Common DTOs, constants, and utilities
├── docs/              # API documentation and technical notes
├── docker-compose.yml # Infrastructure (PostgreSQL, Redis, RabbitMQ, MinIO)
└── pom.xml            # Parent Maven configuration
```

## 📋 Requirements

- **Java**: 21
- **Maven**: 3.9+
- **Docker & Docker Compose**: For infrastructure services

## 🚦 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/your-repo/orang-chat.git
cd orang-chat
```

### 2. Start Infrastructure
Use Docker Compose to start the required databases and services:
```bash
docker-compose up -d
```
This will start:
- 3 PostgreSQL instances (Auth, User, Message databases)
- Redis
- RabbitMQ
- MinIO

### 3. Build the project
Build all modules using the Maven wrapper:
```bash
./mvnw clean install
```

### 4. Run Services
Start the services in the following order:
1. **API Gateway**: `cd api-gateway && ../mvnw spring-boot:run`
2. **Auth Service**: `cd auth-service && ../mvnw spring-boot:run`
3. **User Service**: `cd user-service && ../mvnw spring-boot:run`
4. **Message Service**: `cd message-service && ../mvnw spring-boot:run`
5. **Chat Service**: `cd chat-service && ../mvnw spring-boot:run`

## ⚙️ Environment Variables

The following environment variables can be configured:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `DB_USERNAME` | `authuser` / `useruser` | Database username |
| `DB_PASSWORD` | `authpass` / `userpass` | Database password |
| `JWT_SECRET` | `myVeryLongSecretKey...` | Secret key for JWT signing |

## 🧪 Testing

Run tests for all modules from the root directory:
```bash
./mvnw test
```

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details (TODO: Add LICENSE file).

---
*Note: This project is currently under active development. Real-time messaging, conversation history, and read-receipts are now implemented.*
