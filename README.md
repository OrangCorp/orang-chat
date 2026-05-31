# Orang Chat 🍊

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-brightgreen?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot 3.5" />
  <img src="https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react&logoColor=white" alt="React 18" />
  <img src="https://img.shields.io/badge/Docker-Supported-blue?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License MIT" />
</p>

<p align="center">
  <strong>A cloud-native, microservices-driven real-time chat application built for "orang" souls.</strong>
</p>

---

## 🚀 About

**Orang Chat** is a modern web-based communication platform developed as a college project. Built with a robust cloud-native microservices architecture, the application is engineered from the ground up for high scalability, fault tolerance, ultra-low latency real-time communication, and enterprise-grade reliability.

---

## ✨ Features

| Feature | Description |
| :--- | :--- |
| 💬 **Rich Messaging** | Seamless 1-1 and group chats equipped with a flexible group role and permissions system. |
| ⚡ **Real-Time Pipeline** | Instant message delivery, typing indicators, and presence tracking powered by WebSockets. |
| 📝 **Message Controls** | Human-centric design allowing full editing and deletion (un-sending) of messages. |
| 📎 **Smart Attachments** | Supports file uploads up to 50MB (max 250MB per message) with automatic server-side image/GIF thumbnailing. |
| 🗂️ **Read Receipts** | Persistent read-status tracking so you can seamlessly pick up exactly where you left off. |
| ❤️ **Reactions & Expressiveness** | Express yourself instantly with emoji-based message reactions. |
| 🔔 **Push Notifications** | Background alerts via Web Push API (VAPID) to keep you informed even when the browser tab is closed. |
| 🛡️ **Account & Safety** | Secure authentication, customizable profiles, robust contact request management, and user blocking. |

---

## 🏗️ Architecture Overview

### 🎨 Frontend

The frontend is a cutting-edge Single Page Application (SPA) optimized for speed and fluid user experiences. 

* **UI/UX Layer:** Built with **React 18** and scaffolded via **Vite**, utilizing **Material UI (MUI)** for a polished, accessible, and responsive user interface.
* **State & Routing:** Component rendering and protected views are managed safely by **React Router v6** via an `AuthLayout` (unauthenticated) and `MainLayout` (authenticated) architecture.
* **Real-Time Data:** Powered by `chatService.js`, which encapsulates **STOMP.js over WebSockets** to pipe messages, typing status, and presence updates directly from the RabbitMQ broker without polling.
* **Background Workers:** Integrates a native **Service Worker** to handle Web Push notifications globally, appending updates directly into a header-docked notification tray.

#### Frontend Tech Stack
* React 18 & Vite
* Material UI (MUI)
* React Router v6
* STOMP.js (WebSocket client)
* React Hook Form

---

### ⚙️ Backend

The backend engine orchestrates business logic, complex data persistence, and inter-service events using **Java 21** (leveraging virtual threads for high concurrency) and **Spring Boot 3.5**.

* **Gateway & Routing:** **Spring Cloud Gateway** acts as the single entry point, managing service routing, security enforcement, and rate limiting.
* **Asynchronous Architecture:** Microservices communicate asynchronously via **RabbitMQ**, establishing a decoupled, event-driven ecosystem.
* **Storage Strategy:** Structured relational data is kept in **PostgreSQL** (optimized with GIN indexes for fast full-text searching over 1M+ messages), while **Redis** acts as a lightning-fast session and JWT blacklist cache.
* **Object Storage:** **MinIO** provides S3-compatible, distributed object storage for attachments, paired with async backend workers for image processing.

#### Backend Tech Stack

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 21 | Modern JVM utilizing Virtual Threads |
| **Framework** | Spring Boot | 3.5.11 | Core enterprise application framework |
| **Cloud** | Spring Cloud | 2025.0.1 | Microservices discovery & orchestration |
| **Gateway** | Spring Cloud Gateway | *Latest* | Edge routing, security filtering, and rate limiting |
| **Database** | PostgreSQL | 15+ | Relational data persistence with advanced GIN indexing |
| **Cache** | Redis | 7.0+ | Session storage, presence tracking, and JWT blacklisting |
| **Broker** | RabbitMQ | 4.0+ | Distributed message broker for event-driven workflows |
| **Storage** | MinIO | 8.5.7 | S3-compatible distributed object storage for assets |
| **Security** | Spring Security | 6.x | Enterprise authentication & role-based authorization |
| **ORM** | Hibernate | 6.x | Java Persistence API (JPA) data mapping |
| **Migrations**| Flyway | *Latest* | Automated, version-controlled database schema migrations |
| **Auth** | JJWT | 0.12.5 | Secure stateless JWT token generation & verification |
| **Web Push** | web-push | 5.1.1 | VAPID protocol implementation for browser push |
| **API Docs** | SpringDoc OpenAPI | 2.8.16 | Automated Swagger UI / OpenAPI 3.0 generation |
| **Testing** | JUnit 5 + Testcontainers | *Latest* | Isolated integration and unit testing suites |
| **Build** | Maven | 3.9+ | Dependency management and build automation |

---

## 🛠️ Getting Started & Deployment

### Local Development
The application is fully containerized for trivial local initialization. Docker Compose configurations and a quick-start guide can be found within the setup directory:

```bash
cd ./Run
# Refer to the README.md within this directory for environment variables and execution scripts
```

## 📦 Production Notes
Production deployment environments assume decoupled external database instances and enterprise SMTP mailing integrations.

> 💡 **Database Migration:** For step-by-step instructions on running Flyway schemas and migrating data safely from local Docker setups to cloud-managed production environments, please consult the migration guides inside the `docs/` folder.

---

## 📂 Documentation Structure
Comprehensive technical overviews are structured into localized folders across the repository:

* 📁 `docs/` — Comprehensive design breakdowns for the Backend, Frontend, and Cloud Infrastructure.
* 📁 `security_docs/` — Security whitepapers detailing encryption, JWT lifecycle, verification mechanics, and vulnerability mitigations.

---

## 📄 License
This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for complete details.
