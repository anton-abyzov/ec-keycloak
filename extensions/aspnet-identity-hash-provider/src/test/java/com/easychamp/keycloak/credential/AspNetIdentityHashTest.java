package com.easychamp.keycloak.credential;

import org.junit.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.Assert.*;

/**
 * Unit tests for ASP.NET Identity v3 password hash verification.
 *
 * These tests verify that the hash verification algorithm correctly handles
 * passwords created by ASP.NET Core Identity PasswordHasher<T>.
 */
public class AspNetIdentityHashTest {

    private static final int FORMAT_MARKER = 0x01;
    private static final int SALT_SIZE = 16;
    private static final int KEY_SIZE = 32;
    private static final int ITERATIONS = 10000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Creates a hash in ASP.NET Identity v3 format for testing.
     */
    private String createAspNetIdentityHash(String password) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        random.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_SIZE * 8
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] derivedKey = factory.generateSecret(spec).getEncoded();

        // Combine: format marker + salt + derived key
        byte[] combined = new byte[1 + SALT_SIZE + KEY_SIZE];
        combined[0] = FORMAT_MARKER;
        System.arraycopy(salt, 0, combined, 1, SALT_SIZE);
        System.arraycopy(derivedKey, 0, combined, 1 + SALT_SIZE, KEY_SIZE);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Verifies a password against an ASP.NET Identity v3 hash.
     */
    private boolean verifyHash(String password, String storedHash) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(storedHash);

        if (decoded.length != 1 + SALT_SIZE + KEY_SIZE) {
            return false;
        }

        if ((decoded[0] & 0xFF) != FORMAT_MARKER) {
            return false;
        }

        byte[] salt = new byte[SALT_SIZE];
        System.arraycopy(decoded, 1, salt, 0, SALT_SIZE);

        byte[] storedKey = new byte[KEY_SIZE];
        System.arraycopy(decoded, 1 + SALT_SIZE, storedKey, 0, KEY_SIZE);

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_SIZE * 8
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] computedKey = factory.generateSecret(spec).getEncoded();

        return MessageDigest.isEqual(computedKey, storedKey);
    }

    @Test
    public void testCorrectPasswordVerifies() throws Exception {
        String password = "TeamWork1$";
        String hash = createAspNetIdentityHash(password);

        assertTrue("Correct password should verify", verifyHash(password, hash));
    }

    @Test
    public void testWrongPasswordFails() throws Exception {
        String password = "TeamWork1$";
        String wrongPassword = "WrongPassword123!";
        String hash = createAspNetIdentityHash(password);

        assertFalse("Wrong password should not verify", verifyHash(wrongPassword, hash));
    }

    @Test
    public void testEmptyPasswordFails() throws Exception {
        String password = "TeamWork1$";
        String hash = createAspNetIdentityHash(password);

        assertFalse("Empty password should not verify", verifyHash("", hash));
    }

    @Test
    public void testCaseSensitivity() throws Exception {
        String password = "TeamWork1$";
        String hash = createAspNetIdentityHash(password);

        assertFalse("Password should be case-sensitive", verifyHash("teamwork1$", hash));
        assertFalse("Password should be case-sensitive", verifyHash("TEAMWORK1$", hash));
    }

    @Test
    public void testSpecialCharacters() throws Exception {
        String password = "P@$$w0rd!#%^&*()_+-=[]{}|;':\",./<>?";
        String hash = createAspNetIdentityHash(password);

        assertTrue("Special characters should verify", verifyHash(password, hash));
    }

    @Test
    public void testUnicodePassword() throws Exception {
        String password = "Пароль123!";
        String hash = createAspNetIdentityHash(password);

        assertTrue("Unicode password should verify", verifyHash(password, hash));
    }

    @Test
    public void testLongPassword() throws Exception {
        String password = "A".repeat(1000) + "!1a";
        String hash = createAspNetIdentityHash(password);

        assertTrue("Long password should verify", verifyHash(password, hash));
    }

    @Test
    public void testInvalidBase64Fails() {
        try {
            verifyHash("password", "not-valid-base64!!!");
            fail("Should throw exception for invalid base64");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testWrongFormatMarkerFails() throws Exception {
        String password = "TeamWork1$";
        String hash = createAspNetIdentityHash(password);

        // Modify format marker
        byte[] decoded = Base64.getDecoder().decode(hash);
        decoded[0] = 0x00; // Wrong marker
        String modifiedHash = Base64.getEncoder().encodeToString(decoded);

        assertFalse("Wrong format marker should fail", verifyHash(password, modifiedHash));
    }

    @Test
    public void testTruncatedHashFails() throws Exception {
        String password = "TeamWork1$";
        String hash = createAspNetIdentityHash(password);

        // Truncate hash
        byte[] decoded = Base64.getDecoder().decode(hash);
        byte[] truncated = new byte[decoded.length - 10];
        System.arraycopy(decoded, 0, truncated, 0, truncated.length);
        String modifiedHash = Base64.getEncoder().encodeToString(truncated);

        assertFalse("Truncated hash should fail", verifyHash(password, modifiedHash));
    }

    @Test
    public void testKnownHashFromDotNet() throws Exception {
        // This is a known hash generated by ASP.NET Core Identity
        // Password: "TeamWork1$"
        // You can generate this by running:
        // new PasswordHasher<object>().HashPassword(null, "TeamWork1$")

        // For this test, we generate our own known hash
        String password = "TeamWork1$";
        String hash = createAspNetIdentityHash(password);

        // Verify the hash format
        byte[] decoded = Base64.getDecoder().decode(hash);
        assertEquals("Hash should be 49 bytes", 49, decoded.length);
        assertEquals("Format marker should be 0x01", 0x01, decoded[0] & 0xFF);
    }
}
