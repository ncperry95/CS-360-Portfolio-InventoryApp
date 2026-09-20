package com.snhu.inventoryapp.security;

/**
 * Minimum requirements for a new password. The original app accepted any non
 * empty string, including a single character.
 *
 * Pure Java so it is unit tested on the host. The Activity maps each result to
 * a string resource; this class never builds user facing text.
 */
public final class PasswordPolicy {

    /** Result of checking a candidate password. OK means it passed. */
    public enum Result {
        OK,
        TOO_SHORT,
        TOO_LONG,
        NO_LETTER,
        NO_DIGIT
    }

    public static final int MIN_LENGTH = 8;

    /**
     * Upper bound so a hostile client cannot feed a multi megabyte password
     * into PBKDF2 and tie up the database thread. 128 is well above anything
     * a person types.
     */
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static Result check(char[] password) {
        if (password == null || password.length < MIN_LENGTH) {
            return Result.TOO_SHORT;
        }
        if (password.length > MAX_LENGTH) {
            return Result.TOO_LONG;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter) {
            return Result.NO_LETTER;
        }
        if (!hasDigit) {
            return Result.NO_DIGIT;
        }
        return Result.OK;
    }
}
