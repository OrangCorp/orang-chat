# Technical Debt

[//]: # (## 1. DisplayName Duplication)

[//]: # ()
[//]: # (**Issue:** `displayName` exists in both Auth Service and User Service)

[//]: # ()
[//]: # (**Current State:**)

[//]: # (- Auth Service: Used only during registration)

[//]: # (- User Service: Source of truth after registration)

[//]: # ()
[//]: # (**Problem:**)

[//]: # (- If user updates displayName in User Service, Auth Service copy is stale)

[//]: # (- Auth Service shouldn't need displayName at all &#40;it's profile data&#41;)

[//]: # ()
[//]: # (**Planned Fix:**)

[//]: # (- Remove displayName from Auth Service)

[//]: # (- Auth Service only handles email + password)

[//]: # (- User Service creates profile via RabbitMQ event on registration)

[//]: # (- Demonstrate event-driven microservice communication)

## 2. Contact-Profile Relation
- **Current:** UUID reference, manual lookup
- **Improvement:** Add @ManyToOne relation
- **Benefit:** Simpler code, JPA handles joins