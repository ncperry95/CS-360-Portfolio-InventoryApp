package com.snhu.inventoryapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Host JVM tests for the password hasher. These run with plain JUnit, no
 * device or emulator, because the hasher has no Android dependencies.
 *
 * Iterations are lowered for speed; the format and the guarantees are the
 * same at any count.
 */
public class PasswordHasherTest {

    private static final int TEST_ITERATIONS = 1_000;

    private PasswordHasher hasher;

    @Before
    public void setUp() {
        hasher = new PasswordHasher(new SecureRandom(), TEST_ITERATIONS);
    }

    @Test
    public void hash_producesSelfDescribingFormat() {
        String stored = hasher.hash("correct horse 1".toCharArray());
        String[] parts = stored.split("\\$");
        assertEquals(4, parts.length);
        assertEquals("pbkdf2_sha256", parts[0]);
        assertEquals(String.valueOf(TEST_ITERATIONS), parts[1]);
    }

    @Test
    public void hash_samePasswordTwice_differentSaltsDifferentHashes() {
        char[] password = "same password 9".toCharArray();
        String first = hasher.hash(password.clone());
        String second = hasher.hash(password.clone());
        assertNotEquals("two users with the same password must not share a hash", first, second);
        assertTrue(hasher.verify(password.clone(), first));
        assertTrue(hasher.verify(password.clone(), second));
    }

    @Test
    public void verify_acceptsCorrectPassword() {
        String stored = hasher.hash("Tr0ub4dor&3".toCharArray());
        assertTrue(hasher.verify("Tr0ub4dor&3".toCharArray(), stored));
    }

    @Test
    public void verify_rejectsWrongPassword() {
        String stored = hasher.hash("Tr0ub4dor&3".toCharArray());
        assertFalse(hasher.verify("Tr0ub4dor&4".toCharArray(), stored));
        assertFalse(hasher.verify("tr0ub4dor&3".toCharArray(), stored));
        assertFalse(hasher.verify("".toCharArray(), stored));
    }

    @Test
    public void verify_rejectsMalformedStoredValues() {
        char[] password = "whatever123".toCharArray();
        assertFalse(hasher.verify(password.clone(), null));
        assertFalse(hasher.verify(password.clone(), ""));
        assertFalse(hasher.verify(password.clone(), "pbkdf2_sha256$1000$notbase64!!$x"));
        assertFalse(hasher.verify(password.clone(), "md5$1$abc$def"));
        assertFalse(hasher.verify(password.clone(), "pbkdf2_sha256$0$AAAA$AAAA"));
    }

    @Test
    public void hash_neverReturnsInput() {
        String password = "plaintext123";
        String stored = hasher.hash(password.toCharArray());
        assertNotEquals(password, stored);
        assertFalse(stored.contains(password));
    }

    @Test
    public void hash_emptyPassword_throwsInsteadOfFallingBack() {
        try {
            hasher.hash(new char[0]);
            fail("empty password must be rejected");
        } catch (IllegalArgumentException expected) {
            // fail closed
        }
    }

    @Test
    public void verify_honoursIterationCountStoredInHash() {
        PasswordHasher slower = new PasswordHasher(new SecureRandom(), TEST_ITERATIONS * 2);
        String stored = slower.hash("iterations 42".toCharArray());
        // A hasher configured with a different default still verifies, because
        // the count is read from the stored value, not from the instance.
        assertTrue(hasher.verify("iterations 42".toCharArray(), stored));
    }

    @Test
    public void needsRehash_trueForLegacyAndLowIterations_falseForCurrent() {
        String legacySha256 = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        assertTrue(hasher.needsRehash(legacySha256));

        PasswordHasher weaker = new PasswordHasher(new SecureRandom(), TEST_ITERATIONS / 2);
        assertTrue(hasher.needsRehash(weaker.hash("abc12345".toCharArray())));

        assertFalse(hasher.needsRehash(hasher.hash("abc12345".toCharArray())));
    }

    @Test
    public void legacy_detectsOnlyExact64HexShape() {
        assertTrue(PasswordHasher.isLegacyHash(
                "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"));
        assertFalse(PasswordHasher.isLegacyHash(
                "5E884898DA28047151D0E56F8DC6292773603D0D6AABBDD62A11EF721D1542D8"));
        assertFalse(PasswordHasher.isLegacyHash("pbkdf2_sha256$1$a$b"));
        assertFalse(PasswordHasher.isLegacyHash("short"));
        assertFalse(PasswordHasher.isLegacyHash(null));
    }

    @Test
    public void legacy_verifiesOriginalSha256Rows() {
        // SHA-256("password") as the original app would have stored it.
        String legacy = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        assertTrue(hasher.verifyLegacy("password".toCharArray(), legacy));
        assertFalse(hasher.verifyLegacy("Password".toCharArray(), legacy));
        // The new verifier must not accept legacy values by accident.
        assertFalse(hasher.verify("password".toCharArray(), legacy));
    }
}
