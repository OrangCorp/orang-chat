# Frontend Architecture

## Overview
The frontend is built with React and Vite, following a modular component-based architecture with modern React patterns and state management.

 
---

## System Architecture
 
## Directory Structure

src/
├── assets/            # Static assets (icons, logos)
|     ├──  styles/ # Themes
├── components/          # Reusable UI components
|        ├── chat/           # Chat-specific components (message bubble)
|        ├── common/         # Universal components (header, sidebar)
├── context/            # React Context providers for use elsewhere
├── layouts/            # Layout wrapper components 
├── pages/              # Page screen components (Chat, login, profile ,etc.)
├── routes/             # Route definitions and guards
├── services/           # API and external service calls
├── types/              # TypeScript type definitions
├── utils/              # Miscellaneous utility functions and constants
|
├── App.jsx             # Main application component
└── main.jsx            # Application entry point



---

### Routing

React Router with two layouts:

| Layout | Routes |
|--------|--------|
| **AuthLayout** | `/login`, `/signup`, `/verify-email`, `/reset-password` |
| **MainLayout** (sidebar + header) | `/chat/:id`, `/profile/:id`, `/settings`, `/` |

`PrivateRoute.jsx` wraps protected routes — checks authentication, redirects to `/login` if not authenticated.

---

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

### Authentication & Token Management

The app uses a *JWT access/refresh token* pair stored in localStorage. The authService.js singleton manages the full lifecycle:

| Token | Lifetime | Storage | Purpose |
|-------|----------|---------|---------|
| Access token | ~15 minutes | localStorage | Authenticates API requests via Authorization: Bearer header |
| Refresh token | Longer-lived | localStorage | Obtains new access tokens without re-login |

*Token refresh* happens proactively — a timer fires 60 seconds before the access token expires, calling /api/auth/refresh. If the refresh succeeds, the new tokens replace the old ones silently. The user never sees this.

*When refresh fails* (expired refresh token, network error, server rejects):
- All auth data is cleared from localStorage
- isAuthenticated is set to false
- The user is redirected to the login page
- Any cached profile/contact data is purged (userService.clearCache())

*On app startup*, authService.initialize() checks localStorage for existing tokens. If an access token is expired, it attempts a refresh immediately. If that fails, the user starts unauthenticated and sees the login page.

---

### Route Protection (PrivateRoute.jsx)

All authenticated routes are wrapped in a PrivateRoute component:

User navigates to /chat/:id
        │
        ▼

 Is attemptedAuth true? ── No ──► Show loading spinner
 (auth initialized yet?) 
           │ Yes
           ▼
 Is isAuthenticated true?── No ──► Redirect to /login
           │ Yes
           ▼
     Render the page

This prevents unauthenticated users from accessing any page behind the MainLayout. The attemptedAuth flag prevents a flash of the login page while tokens are being validated on initial load.

---

### API Request Authentication

Every API call goes through service classes that attach the JWT via a shared helper:

const getHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

If a request returns *401 Unauthorized*, the user's auth state is cleared and they're redirected to login. There's no automatic retry — the refresh timer handles keeping the token fresh proactively.

---

### Server-Side Authorization (Backend Enforced)

The frontend sends the JWT with every request, but *the backend is the authority* on whether a user can access a resource. Examples:

- *Accessing a conversation you're not a member of:* The backend returns 403 Forbidden. The frontend displays an error message.
- *Editing/deleting someone else's message:* The backend rejects it. The frontend only shows edit/delete buttons on your own messages (isOwn flag).
- *Group admin actions (kick, promote, rename):* The UI only shows these controls to users with the ADMIN role, but the backend validates again on every request.

The frontend uses UI-level hiding as a convenience, not as security.

---

### WebSocket Authentication

The STOMP WebSocket connection authenticates once on connect by passing the JWT in the Authorization header:

connectHeaders: {
  Authorization: `Bearer ${token}`
}

The chat service backend validates this token on CONNECT. If the token expires while connected, the socket disconnects with an error, and the STOMP client's auto-reconnect fires — which uses a fresh token from localStorage (refreshed by the timer). If the refresh also failed and the user was logged out, the reconnection attempt sends no token and is rejected.

*Message-level authorization:* Even after connection, the backend checks that the user is a participant of the conversation before routing messages. For direct messages, it verifies the sender/recipient match. For group messages, it checks group membership.

---

### Push Notification Subscription

Push notifications use the *Web Push API* with VAPID keys:

1. Frontend fetches the VAPID public key from GET /api/push/vapid-public-key (requires JWT)
2. Browser creates a push subscription with FCM (Chrome) or equivalent
3. Subscription details are sent to POST /api/push/subscribe (requires JWT)
4. Backend stores the subscription keyed to the authenticated user

The subscription is tied to both the browser and the VAPID key pair. If VAPID keys change, old subscriptions become invalid (FCM returns 403) and the frontend re-subscribes automatically.

---

### Logout & Cleanup

On logout:
1. POST /api/auth/logout is called (best-effort — errors are swallowed)
2. localStorage is cleared (tokens, user info, expiry)
3. Auth state is reset
4. Profile and contact caches are purged
5. WebSocket is disconnected
6. User is redirected to /login

---

### Security Considerations

| Area | Approach |
|------|----------|
| *Token storage* | localStorage (acceptable for this app; HttpOnly cookies would be stronger but add complexity) |
| *Token lifetime* | Short access tokens (15 min) with silent refresh |
| *XSS* | React's default escaping prevents injection; no dangerouslySetInnerHTML except in search result highlighting (server-provided HTML) |
| *CSRF* | JWTs in Authorization header are not automatically sent by browsers, so CSRF is not a concern |
| *Dev vs Prod logging* | All console.log`/console.error` calls are wrapped with import.meta.env.DEV checks — stripped from production builds |
| *Error messages* | Generic messages shown to users ("Login failed. Please try again."); detailed errors only logged in dev |
### Input Validation

- *Client-side validation*: Immediate feedback using react-hook-form
- *Server validation*: Backend validates all inputs
- *Sanitization*: Prevent XSS with proper escaping
- *Type safety*: TypeScript prevents type-related vulnerabilities

---
## Build & Deployment

- `npm run dev` — Vite dev server at `localhost:5173`
- `npm run build` — outputs static files to `dist/`
- Dockerfile builds the app, copies `dist/` into nginx
- nginx serves React and proxies `/api` (REST) and `/ws` (WebSocket) to the backend gateway
