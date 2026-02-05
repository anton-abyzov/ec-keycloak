# Keycloak Custom Image for EasyChamp
# Based on official Keycloak image with custom theme

FROM quay.io/keycloak/keycloak:26.0 AS builder

# Enable health and metrics
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure database
ENV KC_DB=postgres

# Copy custom theme
COPY docker/themes/easychamp /opt/keycloak/themes/easychamp

# Build optimized Keycloak
RUN /opt/keycloak/bin/kc.sh build

# Production image
FROM quay.io/keycloak/keycloak:26.0

# Copy built artifacts from builder
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Default configuration
ENV KC_DB=postgres
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/health/ready || exit 1

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
