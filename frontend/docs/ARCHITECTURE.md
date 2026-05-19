# Frontend Architecture

## Overview
The frontend is built with React and Vite, following a modular component-based architecture with modern React patterns and state management.

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Design Patterns](#design-patterns)
3. [Data Flow](#data-flow)
4. [Component Architecture](#component-architecture)
5. [State Management](#state-management)
6. [Routing Architecture](#routing-architecture)
7. [API Integration](#api-integration)
8. [Performance Patterns](#performance-patterns)
9. [Security Architecture](#security-architecture)
10. [Build & Deployment](#build--deployment)

---

## System Architecture

### High-Level Overview

┌──────────────────────────────────────────────────────────────────────────┐
│                           ORANG CHAT FRONTEND                            │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   BROWSER LAYER                                 │   │
│  │  Modern Browser (ES6+, WebSocket, Service Worker)              │   │
│  └─────────────────┬───────────────────────────────────────────────┘   │
│                    │                                                    │
│                    ▼                                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              REACT APPLICATION (Vite)                           │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │   │
│  │  │ Context │  │ Router  │  │ Service │  │ Hooks   │            │   │
│  │  │ State   │  │ (React) │  │ Layer   │  │ Layer   │            │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘            │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └─────────┬──────────────┬──────────────┬───────────────────────┘   │
│            │              │              │                           │
│      ┌─────▼────┐  ┌──────▼──────┐ ┌────▼───────┐                  │
│      │   HTTP   │  │   WebSocket │ │   Context  │                  │
│      │ REST API │  │   STOMP     │ │   Updates  │                  │
│      └──────────┘  └─────────────┘ └────────────┘                  │
│            │              │              │                           │
│            ▼              ▼              ▼                           │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐  │
│  │   API Gateway    │ │   Auth Service   │ │   Chat Service   │  │
│  │   (Spring Cloud) │ │   Port: 8081     │ │   Port: 8083     │  │
│  │   Port: 8080     │ │   JWT Tokens     │ │   WebSocket      │  │
│  │   Load Balanced  │ │   User Auth      │ │   Real-time      │  │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘

### Component Hierarchy

App.jsx (Root)
├── Router (React Router)
│   ├── Layout Components
│   │   ├── Header/Navigation
│   │   ├── Sidebar
│   │   └── Footer
│   └── Page Components
│       ├── Auth Pages (Login, Register)
│       ├── Chat Pages (Chat Room, Message History)
│       ├── User Pages (Profile, Settings)
│       └── Error Pages (404, 500)
├── Context Providers
│   ├── AuthContext (JWT, User State)
│   ├── ChatContext (Messages, Presence)
│   └── ThemeContext (UI Preferences)
└── Service Layer
    ├── API Services (REST calls)
    ├── WebSocket Service (Real-time)
    └── Utility Services (Helpers)

## Directory Structure

src/
├── assets/             # Static assets (images, icons)
├── components/          # Reusable UI components
│   ├── common/         # Generic components (Button, Input, etc.)
│   ├── layout/         # Layout components (Header, Sidebar)
│   ├── chat/           # Chat-specific components
│   ├── auth/           # Authentication components
│   └── forms/          # Form components
├── context/            # React Context providers 
├── hooks/              # Custom React hooks 
├── layouts/            # Layout wrapper components
├── pages/              # Page-level components (routes)
├── routes/             # Route definitions and guards
├── services/           # API and external service calls
├── types/              # TypeScript type definitions
├── utils/              # Utility functions and constants
|
├── App.jsx             # Main application component
└── main.jsx            # Application entry point

---

## Design Patterns

### 1. *Component Composition Pattern*
- *Small, focused components*: Each component has single responsibility
- *Composition over inheritance*: Build complex UIs by combining simple components
- *Props drilling prevention*: Use context for shared state

### 2. *Custom Hooks Pattern*
- *Logic extraction*: Move component logic to reusable hooks
- *Side effects*: Handle async operations, subscriptions, timers
- *State management*: Encapsulate related state and actions

### 3. *Context + Reducer Pattern*
- *Global state*: Share state across component tree without props drilling
- *Actions*: Dispatch actions to update state predictably
- *Selectors*: Computed values from state

### 4. *Container/Presentational Pattern*
- *Presentational*: Pure UI components, receive data via props
- *Container*: Handle data fetching, state, side effects
- *Separation*: UI logic separate from business logic

### 5. *Render Props Pattern*
- *Reusable logic*: Share behavior between components
- *Flexibility*: Components can customize rendering
- *Composition*: Alternative to HOCs

### 6. *Compound Components Pattern*
- *Related components*: Group related components together
- *Implicit state*: Share state through context automatically
- *API design*: Clean, intuitive component APIs

### 7. *Provider Pattern*
- *Dependency injection*: Provide services to component tree
- *Configuration*: Centralized app configuration
- *Testing*: Easy to mock providers in tests

---

## Data Flow

### Unidirectional Data Flow

User Interaction → Component Event → Action Dispatch → Context Reducer → State Update → Component Re-render

### Authentication Flow

1. User submits login form
2. AuthService.login() called
3. API request to /auth/login
4. JWT token received and stored
5. AuthContext updated with user data
6. Protected routes become accessible
7. Components re-render with authenticated state

### Real-time Message Flow

1. User sends message
2. ChatService.sendMessage() called
3. WebSocket message sent to server
4. Server broadcasts to other clients
5. WebSocket message received
6. ChatContext updated with new message
7. Message list re-renders

### Error Handling Flow

1. API call fails (network/service error)
2. Service throws error
3. Component catches error
4. Error boundary or error state displayed
5. User notified of issue
6. Retry mechanisms available

---

## Component Architecture

### Atomic Design Principles

- *Atoms*: Basic HTML elements (Button, Input, Icon)
- *Molecules*: Simple combinations (Form Field, Message Item)
- *Organisms*: Complex components (Chat Window, Navigation Bar)
- *Templates*: Page layouts with placeholder content
- *Pages*: Specific instances of templates


### Component Communication

- *Props*: Parent to child data flow
- *Callbacks*: Child to parent communication
- *Context*: Global state sharing
- *Events*: Cross-component communication
- *Refs*: Direct DOM manipulation when needed

---

  
## Security Architecture

### Authentication

- *JWT tokens*: Stored in localStorage with httpOnly cookies as backup
- *Token refresh*: Automatic token renewal before expiration
- *Route protection*: Private routes check authentication status
- *Logout*: Clear tokens and redirect to login

### Authorization

- *Role-based access*: Check user roles for feature access
- *API permissions*: Backend validates permissions on each request
- *UI conditional rendering*: Hide/show features based on permissions

### Security Headers

// Content Security Policy
const csp = {
  'default-src': "'self'",
  'script-src': "'self' 'unsafe-inline'",
  'style-src': "'self' 'unsafe-inline'",
  'img-src': "'self' data: https:",
  'connect-src': "'self' ws: wss:"
};

// Implemented via nginx.conf or meta tags

### Input Validation

- *Client-side validation*: Immediate feedback using react-hook-form
- *Server validation*: Backend validates all inputs
- *Sanitization*: Prevent XSS with proper escaping
- *Type safety*: TypeScript prevents type-related vulnerabilities

---

 
 

This architecture provides a scalable, maintainable, and performant frontend foundation for the Orang Chat application.