package com.snhu.inventoryapp.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing for the Inventory App.
 *
 * Replaces the original unsalted, single round SHA-256 hash with PBKDF2 over
 * HMAC-SHA256, a random per user salt, and a high iteration count. Stored
 * values are self describing strings in the form
 *
 *     pbkdf2_sha256$ITERATIONS$BASE64_SALT$BASE64_HASH
 *
 * so the iteration count can be raised later without a schema change and old
 * rows can still be verified.
 *
 * Design rules this class follows:
 *   - It never returns the input. If hashing fails, it throws. A hashing
 *     failure must be a login failure, not a bypass.
 *   - Comparisons are constant time (MessageDigest.isEqual) so a wrong
 *     password takes the same time to reject no matter where it differs.
 *   - It has no Android dependencies, so it is unit tested on the host JVM.
 *
 * Legacy support: the original app stored 64 character hex SHA-256 digests.
 * {@link #isLegacyHash(String)} and {@link #verifyLegacy(char[], String)} let
 * the repository accept those on a successful login and immediately upgrade
 * the stored value to the new format.
 */
public final class PasswordHasher {

    /** Algorithm name as registered with SecretKeyFactory. Requires Android API 26+. */
    static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Prefix that marks a value produced by this class. */
    static final String FORMAT_ID = "pbkdf2_sha256";

    /**
     * Iteration count for new hashes. This is the tradeoff knob: higher is
     * slower for an attacker but also slower for the user on a phone. 100,000
     * lands around a tenth of a second to a few tenths on current hardware,
     * which is noticeable only because login runs it once.
     */
    static final int DEFAULT_ITERATIONS = 100_000;

    /** 16 byte salt, per NIST SP 800-132 minimum of 128 bits. */
    static final int SALT_LENGTH_BYTES = 16;

    /** 32 byte (256 bit) derived key to match the SHA-256 output size. */
    static final int HASH_LENGTH_BITS = 256;

    private final SecureRandom random;
    private final int iterations;

    public PasswordHasher() {
        this(new SecureRandom(), DEFAULT_ITERATIONS);
    }

    /**
     * Constructor for tests, which can lower the iteration count to keep the
     * suite fast. Production code uses the default.
     */
    PasswordHasher(SecureRandom random, int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.random = random;
        this.iterations = iterations;
    }

    /**
     * Hashes a password with a fresh random salt and returns the encoded string
     * that should be stored in the database.
     *
     * @throws IllegalStateException if the platform cannot perform PBKDF2.
     *         Callers must treat this as a failed operation, never as success.
     */
    public String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password must not be empty");
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        byte[] derived = derive(password, salt, iterations);
        Base64.Encoder enc = Base64.getEncoder();
        return FORMAT_ID + "$" + iterations + "$"
                + enc.encodeToString(salt) + "$"
                + enc.encodeToString(derived);
    }

    /**
     * Checks a password against a stored encoded hash produced by {@link #hash}.
     * Returns false for anything malformed rather than throwing, so a corrupted
     * row cannot crash the login screen, and never returns true unless the
     * derived key matches byte for byte.
     */
    public boolean verify(char[] password, String stored) {
        if (password == null || password.length == 0 || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !FORMAT_ID.equals(parts[0])) {
            return false;
        }
        int storedIterations;
        byte[] salt;
        byte[] expected;
        try {
            storedIterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (storedIterations < 1 || salt.length == 0 || expected.length == 0) {
            return false;
        }
        byte[] actual;
        try {
            actual = derive(password, salt, storedIterations);
        } catch (IllegalStateException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * True when the stored hash needs to be re hashed, either because it is a
     * legacy SHA-256 value or because it was made with fewer iterations than
     * the current default.
     */
    public boolean needsRehash(String stored) {
        if (isLegacyHash(stored)) {
            return true;
        }
        String[] parts = stored == null ? new String[0] : stored.split("\\$");
        if (parts.length != 4) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < iterations;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    // -------------------- Legacy SHA-256 support --------------------

    /**
     * The original app stored an unsalted SHA-256 digest as 64 lowercase hex
     * characters. Anything of that exact shape is treated as legacy.
     */
    public static boolean isLegacyHash(String stored) {
        if (stored == null || stored.length() != 64) {
            return false;
        }
        for (int i = 0; i < stored.length(); i++) {
            char c = stored.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies a password against a legacy SHA-256 hex digest in constant
     * time. Only used to let existing accounts log in once so their stored
     * hash can be upgraded. New hashes are never written in this format.
     */
    public boolean verifyLegacy(char[] password, String storedHex) {
        if (password == null || password.length == 0 || !isLegacyHash(storedHex)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] actual = digest.digest(
                    new String(password).getBytes(StandardCharsets.UTF_8));
            byte[] expected = hexToBytes(storedHex);
            return MessageDigest.isEqual(expected, actual);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    // -------------------- Internals --------------------

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Fail closed. The old code returned the raw password here.
            throw new IllegalStateException("PBKDF2 is unavailable on this device", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(2 * i), 16);
            int lo = Character.digit(hex.charAt(2 * i + 1), 16);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
