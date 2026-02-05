# Keycloak Custom Image for EasyChamp
# Based on official Keycloak image with custom theme and ASP.NET Identity hash provider

# ============================================================================
# Stage 1: Build Java Extensions
# ============================================================================
FROM maven:3.9-eclipse-temurin-17 AS java-builder

WORKDIR /build

# Copy extension source
COPY extensions/aspnet-identity-hash-provider /build/aspnet-identity-hash-provider

# Build the extension JAR
RUN cd /build/aspnet-identity-hash-provider && mvn clean package -DskipTests

# ============================================================================
# Stage 2: Build Keycloak
# ============================================================================
FROM quay.io/keycloak/keycloak:26.0 AS builder

# Enable health and metrics
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure database
ENV KC_DB=postgres

# Enable script protocol mappers feature (for dynamic externalUser claim)
ENV KC_FEATURES=scripts

# Copy custom theme
COPY docker/themes/easychamp /opt/keycloak/themes/easychamp

# Copy Java extensions from builder stage
COPY --from=java-builder /build/aspnet-identity-hash-provider/target/*.jar /opt/keycloak/providers/

# Build optimized Keycloak with extensions and scripts feature
RUN /opt/keycloak/bin/kc.sh build --features=scripts

# ============================================================================
# Stage 3: Production Image
# ============================================================================
FROM quay.io/keycloak/keycloak:26.0

# Copy built artifacts from builder (includes extensions)
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Default configuration
ENV KC_DB=postgres
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false

# Log level (default INFO, can be overridden)
ENV KC_LOG_LEVEL=INFO

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/health/ready || exit 1

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start"]
