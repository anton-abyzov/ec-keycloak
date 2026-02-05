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
 * ASP.NET Core Identity PasswordHasherV3 Format (61 bytes for default config):
 * - Format marker: 0x01 (1 byte)
 * - PRF algorithm: 4 bytes big-endian (0=SHA1, 1=SHA256, 2=SHA512)
 * - Iteration count: 4 bytes big-endian (default: 100,000 for v3)
 * - Salt length: 4 bytes big-endian (default: 16)
 * - Salt: variable (default 16 bytes)
 * - Subkey: variable (default 32 bytes)
 * - Storage: Base64(0x01 + prf[4] + iterations[4] + saltLen[4] + salt[N] + subkey[N])
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
    private static final int HEADER_SIZE = 13; // 1 marker + 4 prf + 4 iterations + 4 saltLength

    // PRF algorithm mapping (from ASP.NET KeyDerivationPrf enum)
    private static final int PRF_SHA1 = 0;
    private static final int PRF_SHA256 = 1;
    private static final int PRF_SHA512 = 2;

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
        boolean configured = legacyHash != null && !legacyHash.isEmpty();
        LOG.info("[ASP.NET SPI] isConfiguredFor user=" + user.getUsername() + " configured=" + configured);
        return configured;
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

        LOG.info("[ASP.NET SPI] isValid called for user=" + user.getUsername() + " hashLength=" + legacyHash.length());

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
     * Verify password against ASP.NET Core Identity PasswordHasherV3 format.
     *
     * Format: Base64(0x01 + prf[4] + iterations[4] + saltLen[4] + salt[N] + subkey[N])
     */
    private boolean verifyAspNetIdentityHash(String password, String storedHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(storedHash);
        } catch (IllegalArgumentException e) {
            LOG.warning("[ASP.NET SPI] Invalid Base64 in legacy hash");
            return false;
        }

        // Minimum: 13 bytes header + at least 1 byte salt + 1 byte key
        if (decoded.length < HEADER_SIZE + 2) {
            LOG.warning("Hash too short: " + decoded.length + " bytes (minimum " + (HEADER_SIZE + 2) + ")");
            return false;
        }

        // Check format marker (0x01 for v3)
        if ((decoded[0] & 0xFF) != FORMAT_MARKER) {
            LOG.warning("Invalid format marker: " + (decoded[0] & 0xFF) + " (expected 0x01)");
            return false;
        }

        // Read PRF algorithm (bytes 1-4, big-endian)
        int prf = readNetworkByteOrder(decoded, 1);
        String algorithm = getPbkdf2Algorithm(prf);
        if (algorithm == null) {
            LOG.warning("Unsupported PRF algorithm: " + prf);
            return false;
        }

        // Read iteration count (bytes 5-8, big-endian)
        int iterations = readNetworkByteOrder(decoded, 5);
        if (iterations <= 0) {
            LOG.warning("Invalid iteration count: " + iterations);
            return false;
        }

        // Read salt length (bytes 9-12, big-endian)
        int saltLength = readNetworkByteOrder(decoded, 9);
        if (saltLength < 0 || decoded.length < HEADER_SIZE + saltLength) {
            LOG.warning("Invalid salt length: " + saltLength);
            return false;
        }

        // Extract salt
        byte[] salt = new byte[saltLength];
        System.arraycopy(decoded, HEADER_SIZE, salt, 0, saltLength);

        // Extract stored subkey (everything after salt)
        int subkeyLength = decoded.length - HEADER_SIZE - saltLength;
        byte[] storedKey = new byte[subkeyLength];
        System.arraycopy(decoded, HEADER_SIZE + saltLength, storedKey, 0, subkeyLength);

        LOG.info("[ASP.NET SPI] Hash params - PRF: " + algorithm + ", iterations: " + iterations
            + ", saltLen: " + saltLength + ", subkeyLen: " + subkeyLength);

        // Compute PBKDF2 with parameters from the hash
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            subkeyLength * 8 // bit length
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
        byte[] computedKey = factory.generateSecret(spec).getEncoded();

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(computedKey, storedKey);
    }

    /**
     * Read a 4-byte big-endian (network byte order) integer.
     */
    private static int readNetworkByteOrder(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xFF) << 24)
             | ((buffer[offset + 1] & 0xFF) << 16)
             | ((buffer[offset + 2] & 0xFF) << 8)
             | (buffer[offset + 3] & 0xFF);
    }

    /**
     * Map ASP.NET KeyDerivationPrf enum value to Java PBKDF2 algorithm name.
     */
    private static String getPbkdf2Algorithm(int prf) {
        switch (prf) {
            case PRF_SHA1:   return "PBKDF2WithHmacSHA1";
            case PRF_SHA256: return "PBKDF2WithHmacSHA256";
            case PRF_SHA512: return "PBKDF2WithHmacSHA512";
            default:         return null;
        }
    }

    /**
     * Migrate password to Keycloak's native format after successful validation.
     * Uses Keycloak 26+ API - credentials are accessed via user.credentialManager()
     */
    private void migratePassword(RealmModel realm, UserModel user, String plainPassword) {
        // In Keycloak 26+, use user.credentialManager() instead of session.userCredentialManager()
        user.credentialManager().updateCredential(UserCredentialModel.password(plainPassword, false));
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
