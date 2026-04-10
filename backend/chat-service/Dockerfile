# syntax=docker/dockerfile:1.4
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Copy ONLY the POM files first to leverage Docker layer caching
COPY pom.xml .
COPY shared-library/pom.xml shared-library/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY chat-service/pom.xml chat-service/
COPY message-service/pom.xml message-service/
COPY api-gateway/pom.xml api-gateway/
COPY notification-service/pom.xml notification-service/

# 2. Pre-fetch dependencies using BuildKit cache
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl chat-service -am -B -q || true

# 3. Copy source code for required modules
COPY shared-library/src shared-library/src
COPY chat-service/src chat-service/src

# 4. Build with BuildKit cache and parallel threads
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl chat-service -am -DskipTests -T 1C -B -q

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

RUN addgroup -S javauser && adduser -S javauser -G javauser
USER javauser:javauser

COPY --from=builder /build/chat-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]