# Infrastructure Memory

<!-- Project-specific learnings for this skill -->

## Learnings

- **2026-02-06**: Keycloak CSS caching: KC_SPI_THEME_STATIC_MAX_AGE=-1 reduces Cloudflare cache to 4hr; also rename CSS file to bust existing 30-day cache
- **2026-02-06**: Keycloak theme CSS: .pf-c-button.pf-m-control matches both password toggle AND social buttons - scope password toggle to .pf-c-input-group to avoid affecting social buttons
- **2026-02-06**: Keycloak parent theme #kc-info-wrapper has background: #f0f0f0 - override with background: transparent !important when customizing login theme
