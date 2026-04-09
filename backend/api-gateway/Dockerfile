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

# 2. Pre-fetch dependencies using BuildKit cache.
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl api-gateway -am -B -q || true

# 3. ONLY copy the source code needed for the modules being built
COPY shared-library/src shared-library/src
COPY api-gateway/src api-gateway/src

# 4. Build the app with BuildKit cache and parallel threads
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -pl api-gateway -am -DskipTests -T 1C -B -q

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

RUN addgroup -S javauser && adduser -S javauser -G javauser
USER javauser:javauser

COPY --from=builder /build/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]