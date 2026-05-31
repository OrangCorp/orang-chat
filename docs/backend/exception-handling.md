# Backend Exception Handling

This document describes how backend exception handling works in the Orang Chat project.

## Shared global exception handling

The backend uses a centralized handler in `backend/shared-library/src/main/java/com/orang/shared/exception/GlobalExceptionHandler.java`.
This class is annotated with `@RestControllerAdvice` and converts exceptions thrown by controller or service code into consistent HTTP JSON responses.

## Error response format

All handled errors are returned as an `ErrorResponse` payload from `backend/shared-library/src/main/java/com/orang/shared/exception/ErrorResponse.java`.
The payload contains:

- `timestamp` — when the error occurred
- `status` — HTTP status code
- `error` — HTTP reason phrase or error name
- `message` — human-readable error detail
- `path` — request URI
- `validationErrors` — optional map of field validation issues

## Custom exception handling

Application-specific exceptions extend `BaseException` and carry an `HttpStatus`.
`GlobalExceptionHandler` handles these custom exceptions and maps them to the proper response status.

Key handlers include:

- `BaseException` → uses the exception's own HTTP status and message
- `EmailServiceException` → logs the email error type and returns `503 SERVICE_UNAVAILABLE`
- `MethodArgumentNotValidException` → returns `400 BAD_REQUEST` with field-level validation errors
- `NoResourceFoundException` / `NoHandlerFoundException` → returns `404 NOT_FOUND`
- `HttpRequestMethodNotSupportedException` → returns `405 METHOD_NOT_ALLOWED`

## Fallback handling

A catch-all handler for `Exception.class` returns a generic `500 INTERNAL_SERVER_ERROR` response.
This prevents unexpected exceptions from leaking raw stack traces and ensures every request gets a valid JSON error response.

## Design benefits

- Centralized error handling across backend services
- Consistent error payloads for frontend/API clients
- Clear separation between application errors and generic server failures
- Reusable behavior via shared library across all backend modules
