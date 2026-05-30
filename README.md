# Orang Chat 🍊
A chat application for orang souls
# About
 Orang chat is a web chat application designed as part of a college project.
 It boasts the following features
## Features
### One-on-one and group chats
The app allows for both one-on-one and group chats with a simple role system for the latter

### Simple account and activity system 
There is a simple account system with basic customisation and a blocking feature  In addition, user activity is also tracked and displayed for other

### Real time chatting 
Users can chat with each other in real time with updates being sent out immediately after a new message appears

### Message editing and deletion
The app design eknowledges human fallibility and thus allows for both editing and removal of messages.

### File attatchments with thumbnails
Users can send each other files up to 50MB in size (max 250MB total per message) with thumbnails generated for select formats such as .png and .gif

### Notifications
The app features notifications for important events pertinent to the user allowing, for instance for the user to be instantly updated even on conversations they are not currently browsing

### Message reading status
The app tracks messages seen by users allowing them both to quickly come back to the point in the conversation they were last at 

### Reaction system
Users can add reactions to messages allowing for them to quickly share their feelings about others' messages

# Running
 The docker compose files required to run the application (as well as brief documentation on the topic) are located in the ./Run folder

# Docs
 The documentation files for the backend and frontend, and deployment are located in their respective folders in the docs folder. In addition, a brief security feature overview is located in the security_docs folder

# Brief overview
## Frontend
 The frontend is a single-page application built with React and Vite, using Material UI (MUI) for the component library. It communicates with the backend through REST APIs for data operations and STOMP over WebSocket for real-time messaging. The app is served in production by nginx, which also proxies API and WebSocket traffic to the backend gateway.

 The interface is split into two main layouts: an AuthLayout for unauthenticated pages (login, signup, email verification) and a MainLayout for the authenticated experience. The MainLayout consists of a persistent Header with search, notifications, and profile controls, a collapsible Sidebar listing conversations, and a content area that renders the active page — typically the Chat view, user Profile pages, or Settings. Navigation between chats, profiles, and settings is handled by React Router with a PrivateRoute wrapper that redirects unauthenticated users to login.

 Real-time chat is the core feature. Messages, typing indicators, and presence updates flow through a single WebSocket connection managed by chatService.js, which wraps the STOMP protocol over RabbitMQ. Messages appear instantly for all participants without polling. Users can send text, attachments (images, files), and emoji reactions. The sidebar updates live when new conversations are created or when push notifications arrive about group additions or direct chats. A search feature lets users find specific messages and jump to their context in the conversation.

 The app also handles contacts and push notifications. Users can search for others, send contact requests, and manage their contact list. Push notifications (via the Web Push API and a service worker) deliver real-time alerts for new messages, reactions, mentions, and contact requests — even when the browser tab is in the background. A notification inbox accessible from the Header stores persistent notifications, supports marking as read or deleting, and allows accepting or declining contact requests directly from the dropdown.

### Tech Stack

- React 18
- Vite
- Material UI
- React Router v6
- STOMP.js for WebSocket
- React Hook Form

## Backend
### A modern, cloud-native microservices chat application built with **Java 21**, **Spring Boot 3.5**, and **Spring Cloud**. Designed for scalability, real-time communication, and reliability.
### The orang chat backend handles: 
✅ **Authentication & Authorization** - JWT tokens, password reset, email verification
✅ **User Management** - Profiles, contacts, friend requests, blocking, presence tracking
✅ **Real-Time Messaging** - WebSocket/STOMP with horizontal scaling via RabbitMQ
✅ **Persistent Storage** - Messages, conversations, reactions, pins, read receipts
✅ **File Attachments** - MinIO S3-compatible storage with async thumbnail generation
✅ **Web Push Notifications** - Native browser notifications with VAPID protocol
✅ **Event-Driven Architecture** - Loose coupling via RabbitMQ message broker
✅ **Full-Text Search** - PostgreSQL GIN indexes for searching 1M+ messages
### 🛠 Tech Stack

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


## Deployment
### The orang chat app is fully containerised with separate setups for fully local development deployments as well as production setups which assume external databases and mailing services. Instructions for migrating databases when moving from the dev setup to the prod one have been provided

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.