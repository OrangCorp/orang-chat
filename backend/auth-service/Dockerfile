# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy all POM files to resolve dependencies
COPY pom.xml ./pom.xml
COPY shared-library/pom.xml ./shared-library/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY chat-service/pom.xml ./chat-service/pom.xml
COPY message-service/pom.xml ./message-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml

# Copy source code
COPY shared-library/src ./shared-library/src
COPY auth-service/src ./auth-service/src

# Build
RUN mvn clean package -pl auth-service -am -DskipTests -q

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

COPY --from=builder /build/auth-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]