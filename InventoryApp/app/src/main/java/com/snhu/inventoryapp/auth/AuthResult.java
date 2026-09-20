package com.snhu.inventoryapp.auth;

import com.snhu.inventoryapp.security.PasswordPolicy;

/**
 * Outcome of a login or account creation attempt. Carries either a user id on
 * success or an error code the UI maps to a string resource.
 *
 * The repository never produces user facing text; that stays in the Activity
 * where resources live.
 */
public final class AuthResult {

    public enum Error {
        NONE,
        FIELDS_REQUIRED,
        INVALID_CREDENTIALS,
        USERNAME_TAKEN,
        USERNAME_TOO_LONG,
        PASSWORD_TOO_SHORT,
        PASSWORD_TOO_LONG,
        PASSWORD_NO_LETTER,
        PASSWORD_NO_DIGIT,
        STORAGE_FAILED
    }

    private final long userId;
    private final String username;
    private final Error error;

    private AuthResult(long userId, String username, Error error) {
        this.userId = userId;
        this.username = username;
        this.error = error;
    }

    public static AuthResult success(long userId, String username) {
        return new AuthResult(userId, username, Error.NONE);
    }

    public static AuthResult failure(Error error) {
        return new AuthResult(SessionManager.NO_USER, null, error);
    }

    /** Maps a password policy result onto the matching auth error. */
    public static AuthResult fromPolicy(PasswordPolicy.Result result) {
        switch (result) {
            case TOO_SHORT:
                return failure(Error.PASSWORD_TOO_SHORT);
            case TOO_LONG:
                return failure(Error.PASSWORD_TOO_LONG);
            case NO_LETTER:
                return failure(Error.PASSWORD_NO_LETTER);
            case NO_DIGIT:
                return failure(Error.PASSWORD_NO_DIGIT);
            default:
                throw new IllegalArgumentException("OK is not a failure");
        }
    }

    public boolean isSuccess() {
        return error == Error.NONE;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Error getError() {
        return error;
    }
}
