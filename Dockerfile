# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cache mount persists across builds, even --no-cache image rebuilds)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy frontend configuration files
COPY package.json .
COPY package-lock.json .
COPY webpack.config.js .

# Copy source code
COPY src src
COPY mvnw .
COPY .mvn .mvn

# Build the application, skipping tests
# (npm ci/install happens inside this goal via frontend-maven-plugin, so npm's cache
#  also benefits from mounting node's download cache)
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Patch OS-level packages (OpenSSL etc.) — this is the stage that actually ships,
# so this is where the Trivy-flagged CVEs get resolved.
RUN apk update && apk upgrade --no-cache

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory and set ownership
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

# Expose port
EXPOSE 8091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8091/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
