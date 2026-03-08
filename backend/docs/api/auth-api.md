# Auth Service API Documentation

Base URL (direct): `http://localhost:8081`
Base URL (via gateway): `http://localhost:8080`

## Endpoints

### 1. Register User

**POST** `/api/auth/register`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "displayName": "John Doe"
}
```

**Response (201 Created):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "John Doe",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**Errors:**
 * `400 Bad Request` - Validation failed
 * `409 Conflict` - Email already registered

### 2. Login
   **POST** `/api/auth/login`

**Request:**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
Same as registration response

**Errors:**

`400 Bad Request` - Validation failed
`401 Unauthorized` - Invalid credentials

### 3. Health Check

   **GET** `/actuator/health`

**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"}
  }
}
```

### Authentication

For protected endpoints (coming in other services), include the token in the header:

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Error Response Format

```json
{
  "timestamp": "2026-03-06T17:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters"
  }
}
```