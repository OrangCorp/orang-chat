# Technical Debt

This document tracks known backend debt based on current code.

## 1) Profile Data Ownership Split

Issue:
- displayName exists in both auth-service user entity and user-service profile entity.

Risk:
- Divergence after profile updates.

Recommended direction:
- Keep profile fields (displayName/avatar/bio) owned by user-service only.
- Auth service should only own identity/authentication data.

## 2) Silent JWT Filter Failures (user-service)

Issue:
- JWT filter in user-service catches Exception and ignores it. (Fixed in message-service).

Risk:
- Missing diagnostics and ambiguous auth failures in user-service.

Recommended direction:
- Log token parsing failures at debug/warn level with safe details.
- Keep request unauthenticated when token invalid, but observable in logs.

## 3) WebSocket Security Exception Semantics

Issue:
- STOMP CONNECT auth in chat-service throws generic RuntimeException.

Risk:
- Weak error semantics and inconsistent handling.

Recommended direction:
- Throw explicit authentication/authorization exceptions.
- Standardize WebSocket error payload strategy for clients.

## 4) WebSocket CORS is Wide Open

Issue:
- setAllowedOriginPatterns("*") in WebSocket endpoint config.

Risk:
- Production exposure if perimeter controls are weak.

Recommended direction:
- Restrict origins by environment config.

## 5) Messaging Feature Gaps

Issue:
- Advanced search filters (e.g. by sender, date range) and complex media processing (video transcoding, large file chunking) are not implemented.
- Thumbnail generation is only supported for image types (JPEG, PNG).

Risk:
- Product feature incompleteness.

Recommended direction:
- Identify missing niche features based on user feedback.
- Extend models/APIs as needed.

## 6) Test Coverage Imbalance

Issue:
- Good auth/presence tests, but sparse coverage in message/chat critical paths.

Risk:
- Regressions in async messaging, role transitions, and websocket flows.

Recommended direction:
- Prioritize unit/integration tests for ContactService, ConversationService, ChatMessageListener, and WebSocket handlers.

## 7) Migration Strategy Consistency

Issue:
- Migration approach is not fully standardized across services.

Risk:
- Environment drift and harder rollback/versioning.

Recommended direction:
- Standardize on Flyway or Liquibase across services and CI.