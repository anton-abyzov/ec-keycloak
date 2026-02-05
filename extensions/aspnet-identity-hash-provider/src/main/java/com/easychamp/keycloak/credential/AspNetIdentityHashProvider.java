package com.easychamp.keycloak.credential;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keycloak Credential Provider for ASP.NET Core Identity v3 password hashes.
 *
 * ASP.NET Core Identity v3 Format:
 * - Algorithm: PBKDF2 with HMAC-SHA256
 * - Iterations: 10,000
 * - Salt: 16 bytes (128 bits)
 * - Derived Key: 32 bytes (256 bits)
 * - Storage Format: Base64(0x01 + salt[16] + derivedKey[32])
 *
 * This provider:
 * 1. Checks if user has 'legacyPasswordHash' attribute
 * 2. Validates password against the legacy hash
 * 3. On success, creates a new Keycloak password credential
 * 4. Removes the legacy hash attribute
 */
public class AspNetIdentityHashProvider implements CredentialProvider<PasswordCredentialModel>, CredentialInputValidator {

    private static final Logger LOG = Logger.getLogger(AspNetIdentityHashProvider.class.getName());

    public static final String PROVIDER_ID = "aspnet-identity-hash";
    public static final String LEGACY_HASH_ATTRIBUTE = "legacyPasswordHash";

    // ASP.NET Identity v3 constants
    private static final int FORMAT_MARKER = 0x01;
    private static final int SALT_SIZE = 16;
    private static final int KEY_SIZE = 32;
    private static final int ITERATIONS = 10000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final KeycloakSession session;

    public AspNetIdentityHashProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return PasswordCredentialModel.TYPE;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) {
            return false;
        }
        // Check if user has legacy password hash attribute
        String legacyHash = user.getFirstAttribute(LEGACY_HASH_ATTRIBUTE);
        return legacyHash != null && !legacyHash.isEmpty();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!(credentialInput instanceof UserCredentialModel)) {
            LOG.fine("Credential input is not UserCredentialModel");
            return false;
        }

        if (!supportsCredentialType(credentialInput.getType())) {
            return false;
        }

        String legacyHash = user.getFirstAttribute(LEGACY_HASH_ATTRIBUTE);
        if (legacyHash == null || legacyHash.isEmpty()) {
            LOG.fine("No legacy hash found for user: " + user.getUsername());
            return false;
        }

        String password = credentialInput.getChallengeResponse();
        if (password == null) {
            return false;
        }

        try {
            boolean valid = verifyAspNetIdentityHash(password, legacyHash);

            if (valid) {
                LOG.info("Legacy password validated for user: " + user.getUsername() + " - migrating to Keycloak format");

                // Migrate to Keycloak password format
                migratePassword(realm, user, password);

                // Remove legacy hash attribute
                user.removeAttribute(LEGACY_HASH_ATTRIBUTE);

                LOG.info("Password migration completed for user: " + user.getUsername());
            }

            return valid;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error validating legacy password for user: " + user.getUsername(), e);
            return false;
        }
    }

    /**
     * Verify password against ASP.NET Core Identity v3 hash format.
     *
     * Format: Base64(0x01 + salt[16] + derivedKey[32])
     */
    private boolean verifyAspNetIdentityHash(String password, String storedHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(storedHash);
        } catch (IllegalArgumentException e) {
            LOG.warning("Invalid Base64 in legacy hash");
            return false;
        }

        // Validate format: 1 byte marker + 16 bytes salt + 32 bytes key = 49 bytes
        if (decoded.length != 1 + SALT_SIZE + KEY_SIZE) {
            LOG.warning("Invalid hash length: " + decoded.length + " (expected 49)");
            return false;
        }

        // Check format marker (0x01 for v3)
        if ((decoded[0] & 0xFF) != FORMAT_MARKER) {
            LOG.warning("Invalid format marker: " + (decoded[0] & 0xFF) + " (expected 0x01)");
            return false;
        }

        // Extract salt (bytes 1-16)
        byte[] salt = new byte[SALT_SIZE];
        System.arraycopy(decoded, 1, salt, 0, SALT_SIZE);

        // Extract stored derived key (bytes 17-48)
        byte[] storedKey = new byte[KEY_SIZE];
        System.arraycopy(decoded, 1 + SALT_SIZE, storedKey, 0, KEY_SIZE);

        // Compute PBKDF2 with same parameters
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_SIZE * 8 // bit length
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] computedKey = factory.generateSecret(spec).getEncoded();

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(computedKey, storedKey);
    }

    /**
     * Migrate password to Keycloak's native format after successful validation.
     */
    private void migratePassword(RealmModel realm, UserModel user, String plainPassword) {
        // Use Keycloak's built-in password credential manager
        session.userCredentialManager().updateCredential(
            realm,
            user,
            UserCredentialModel.password(plainPassword, false)
        );
    }

    // Required CredentialProvider methods

    @Override
    public PasswordCredentialModel getCredentialFromModel(org.keycloak.credential.CredentialModel model) {
        return PasswordCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public org.keycloak.credential.CredentialModel createCredential(
            RealmModel realm, UserModel user, PasswordCredentialModel credentialModel) {
        // This provider only validates legacy hashes, not creates them
        // Password creation is delegated to Keycloak's default password provider
        return null;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        // Remove legacy hash attribute if it exists
        String legacyHash = user.getFirstAttribute(LEGACY_HASH_ATTRIBUTE);
        if (legacyHash != null) {
            user.removeAttribute(LEGACY_HASH_ATTRIBUTE);
            return true;
        }
        return false;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext context) {
        return CredentialTypeMetadata.builder()
            .type(getType())
            .category(CredentialTypeMetadata.Category.BASIC_AUTHENTICATION)
            .displayName("aspnet-identity-hash-display")
            .helpText("aspnet-identity-hash-help")
            .removeable(true)
            .build(session);
    }
}
