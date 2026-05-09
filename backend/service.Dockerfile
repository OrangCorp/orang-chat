# syntax=docker/dockerfile:1.4
ARG SERVICE_NAME
ARG SERVICE_PORT

FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
ARG SERVICE_NAME

WORKDIR /build

# 1. Copy all POMs to cache dependencies
COPY pom.xml .
COPY shared-library/pom.xml shared-library/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY chat-service/pom.xml chat-service/
COPY message-service/pom.xml message-service/
COPY api-gateway/pom.xml api-gateway/
COPY notification-service/pom.xml notification-service/

# 2. Pre-fetch dependencies for this specific service
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl ${SERVICE_NAME} -am -B -q || true

# 3. Copy source code for shared lib and the specific service
COPY shared-library/src shared-library/src
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src

# 4. Build the specific service
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -pl ${SERVICE_NAME} -am -DskipTests -T 1C -B -q

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_PORT
ARG SERVICE_NAME

WORKDIR /app

RUN apk add --no-cache wget

# Security: Run as non-root
RUN addgroup -S javauser && adduser -S javauser -G javauser
USER javauser:javauser

# Copy the built jar from the correct module folder
COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE ${SERVICE_PORT}

# Optimized for container memory limits
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]