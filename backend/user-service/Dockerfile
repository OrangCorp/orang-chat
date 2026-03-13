FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY . .

RUN mvn clean package -pl user-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/user-service/target/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]