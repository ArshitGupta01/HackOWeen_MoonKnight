# syntax=docker/dockerfile:1
# Multi-stage Dockerfile for TheekHai Civic Backend (Ktor + SQLDelight SQLite)

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Install bash and standard utilities for gradle
RUN apk add --no-cache bash

# Copy gradle wrapper and build definitions
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy source projects
COPY core ./core
COPY domain ./domain
COPY data ./data
COPY server ./server

# Build server distribution
RUN chmod +x ./gradlew && ./gradlew :server:installDist --no-daemon --no-configuration-cache

# Stage 2: Production runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN apk add --no-cache curl wget bash

# Copy the installed application from builder
COPY --from=builder /workspace/server/build/install/server /app

# Persistent volume directory for SQLite database
RUN mkdir -p /data && chmod 777 /data
VOLUME ["/data"]

# Environment defaults
ENV PORT=8080
ENV THEEKHAI_DB_PATH=/data/theekhai-server.db
ENV THEEKHAI_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://web-alpha-tawny-78.vercel.app,https://bless-stinger-plow.ngrok-free.dev

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["/app/bin/server"]
