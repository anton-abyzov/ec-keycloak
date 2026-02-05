package com.easychamp.keycloak.credential;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Factory for ASP.NET Identity Password Hash Provider.
 *
 * This factory creates instances of AspNetIdentityHashProvider which can validate
 * passwords stored in ASP.NET Core Identity v3 format (PBKDF2-SHA256).
 *
 * Usage:
 * 1. Users migrated from IS4 have 'legacyPasswordHash' attribute set
 * 2. On login, this provider validates the password against the legacy hash
 * 3. If valid, the password is re-hashed with Keycloak's algorithm
 * 4. The legacy hash is removed, completing the migration
 */
public class AspNetIdentityHashProviderFactory implements CredentialProviderFactory<AspNetIdentityHashProvider> {

    public static final String PROVIDER_ID = "aspnet-identity-hash";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AspNetIdentityHashProvider create(KeycloakSession session) {
        return new AspNetIdentityHashProvider(session);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}
