# EC-Keycloak

Keycloak deployment for EasyChamp platform - Docker image, Helm charts, and Kubernetes manifests.

## Overview

This repository contains:
- Custom Keycloak Docker image with EasyChamp theme
- Helm chart for Kubernetes deployment
- GitHub Actions for CI/CD
- Scripts for realm management

## Architecture

```
easychamp.com
    │
    ├── /ec-keycloak     → Keycloak (this repo)
    ├── /ec-web-ui       → Web Application
    ├── /ec-standings-api → Backend API
    └── /ec-security-api  → Legacy IS4 (to be deprecated)
```

## Repository Structure

```
ec-keycloak/
├── docker/
│   ├── Dockerfile           # Custom Keycloak image
│   └── themes/easychamp/    # Custom theme
├── helm/ec-keycloak/
│   ├── Chart.yaml
│   ├── values.yaml          # Default values
│   ├── values-prod.yaml     # Production overrides
│   └── templates/           # K8s manifests
├── scripts/
│   ├── export-realm.sh
│   └── local-dev.sh
└── .github/workflows/
    ├── build-push.yaml      # Build & push Docker image
    └── notify-argocd.yaml   # Trigger ArgoCD sync
```

## Quick Start

### Local Development

```bash
# Start Keycloak locally with Docker Compose
docker-compose up -d

# Access admin console
open http://localhost:8080/admin
# Default credentials: admin / admin
```

### Deploy to Kubernetes

```bash
# Install via Helm
helm upgrade --install ec-keycloak ./helm/ec-keycloak \
  -n ec-keycloak --create-namespace \
  -f ./helm/ec-keycloak/values-prod.yaml
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KC_DB` | Database type | `postgres` |
| `KC_DB_URL` | JDBC connection URL | - |
| `KC_DB_USERNAME` | Database username | - |
| `KC_DB_PASSWORD` | Database password | - |
| `KC_HOSTNAME` | External hostname | - |
| `KC_PROXY_HEADERS` | Proxy header handling | `xforwarded` |

### Secrets (Kubernetes)

Required secrets:
- `keycloak-db-credentials` - Database connection
- `keycloak-admin` - Admin credentials

## Related Repositories

- [ec-keycloak-realm-config](https://github.com/anton-abyzov/ec-keycloak-realm-config) - Realm configuration (GitOps)
- [ec-gitops](https://github.com/anton-abyzov/ec-gitops) - ArgoCD applications
- [ec-iac-infrastructure](https://github.com/anton-abyzov/ec-iac-infrastructure) - Terraform modules

## Migration from IdentityServer4

This deployment replaces the legacy `ec-security-api` (IdentityServer4). See the migration documentation for details on:
- Client mapping
- User migration
- Token compatibility

## License

Proprietary - EasyChamp
