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

## 2) Contact Removal Bug

Issue:
- ContactService.removeContact validates but does not perform delete.

Risk:
- Endpoint behavior does not match API contract.

Recommended direction:
- Delete entity after authorization checks.
- Add unit test coverage for successful remove and forbidden cases.

## 3) Silent JWT Filter Failures

Issue:
- JWT filters in user-service and message-service catch Exception and ignore it.

Risk:
- Missing diagnostics and ambiguous auth failures.

Recommended direction:
- Log token parsing failures at debug/warn level with safe details.
- Keep request unauthenticated when token invalid, but observable in logs.

## 4) WebSocket Security Exception Semantics

Issue:
- STOMP CONNECT auth in chat-service throws generic RuntimeException.

Risk:
- Weak error semantics and inconsistent handling.

Recommended direction:
- Throw explicit authentication/authorization exceptions.
- Standardize WebSocket error payload strategy for clients.

## 5) WebSocket CORS is Wide Open

Issue:
- setAllowedOriginPatterns("*") in WebSocket endpoint config.

Risk:
- Production exposure if perimeter controls are weak.

Recommended direction:
- Restrict origins by environment config.

## 6) Messaging Feature Gaps

Issue:
- Message lifecycle is read-only after persist (no edit/delete/reaction/read-receipt APIs).

Risk:
- Product feature incompleteness and client workarounds.

Recommended direction:
- Introduce message state transitions with explicit events and APIs.
- Extend MessageType and payload model as needed.

## 7) Test Coverage Imbalance

Issue:
- Good auth/presence tests, but sparse coverage in message/chat critical paths.

Risk:
- Regressions in async messaging, role transitions, and websocket flows.

Recommended direction:
- Prioritize unit/integration tests for ContactService, ConversationService, ChatMessageListener, and WebSocket handlers.

## 8) Migration Strategy Consistency

Issue:
- Migration approach is not fully standardized across services.

Risk:
- Environment drift and harder rollback/versioning.

Recommended direction:
- Standardize on Flyway or Liquibase across services and CI.