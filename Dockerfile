# syntax=docker/dockerfile:1.7
# ──────────────────────────────────────────────────────────────
# Build stage: compile Java + build React frontend
# ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom and download deps (cache layer)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Copy source
COPY src ./src
COPY frontend ./frontend
COPY init.sql ./init.sql

# Build frontend if not already built (pom frontend-plugin handles this)
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    --mount=type=cache,target=/build/frontend/node \
    mvn clean package -DskipTests -B

# ──────────────────────────────────────────────────────────────
# Runtime stage: minimal JRE
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the fat jar
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
