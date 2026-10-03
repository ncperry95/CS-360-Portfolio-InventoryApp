package com.snhu.inventoryapp.data;

import com.snhu.inventoryapp.auth.AuthResult;
import com.snhu.inventoryapp.security.PasswordHasher;
import com.snhu.inventoryapp.security.PasswordPolicy;

import java.util.concurrent.Executor;

/**
 * Everything about accounts: creating one and logging in.
 *
 * Sits between the ViewModels and UserDao so the hashing rules live in
 * exactly one place. Every public method runs its work on the injected
 * Executor and reports back through a callback, so PBKDF2 (which is slow on
 * purpose) never runs on the main thread.
 *
 * Tests pass a direct executor (Runnable::run) to make calls synchronous.
 */
public class UserRepository {

    public static final int MAX_USERNAME_LENGTH = 32;

    private final UserDao users;
    private final PasswordHasher hasher;
    private final Executor executor;

    public UserRepository(UserDao users, PasswordHasher hasher, Executor executor) {
        this.users = users;
        this.hasher = hasher;
        this.executor = executor;
    }

    /**
     * Verifies credentials. On a match with a legacy SHA-256 row, the stored
     * hash is upgraded to PBKDF2 in the same call so the weak value is gone
     * after the user's next successful login.
     */
    public void login(String username, char[] password, RepositoryCallback<AuthResult> callback) {
        String name = normalizeUsername(username);
        if (name.isEmpty() || password == null || password.length == 0) {
            callback.onResult(AuthResult.failure(AuthResult.Error.FIELDS_REQUIRED));
            return;
        }
        executor.execute(() -> {
            try {
                callback.onResult(loginBlocking(name, password));
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });
    }

    /**
     * Creates an account after checking the username and password policy.
     * The uniqueness check and the insert both happen here; a race between
     * two creates for the same name is caught by the UNIQUE constraint and
     * reported as USERNAME_TAKEN.
     */
    public void createAccount(String username, char[] password, RepositoryCallback<AuthResult> callback) {
        String name = normalizeUsername(username);
        if (name.isEmpty() || password == null || password.length == 0) {
            callback.onResult(AuthResult.failure(AuthResult.Error.FIELDS_REQUIRED));
            return;
        }
        if (name.length() > MAX_USERNAME_LENGTH) {
            callback.onResult(AuthResult.failure(AuthResult.Error.USERNAME_TOO_LONG));
            return;
        }
        PasswordPolicy.Result policy = PasswordPolicy.check(password);
        if (policy != PasswordPolicy.Result.OK) {
            callback.onResult(AuthResult.fromPolicy(policy));
            return;
        }
        executor.execute(() -> {
            try {
                callback.onResult(createAccountBlocking(name, password));
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });
    }

    // -------------------- Blocking implementations --------------------

    AuthResult loginBlocking(String username, char[] password) {
        UserEntity user = users.findByUsername(username);
        if (user == null) {
            // Still run one hash so a missing user and a wrong password take
            // similar time. Cheap way to avoid leaking which usernames exist.
            hasher.hash(password);
            return AuthResult.failure(AuthResult.Error.INVALID_CREDENTIALS);
        }

        boolean ok;
        if (PasswordHasher.isLegacyHash(user.passwordHash)) {
            ok = hasher.verifyLegacy(password, user.passwordHash);
        } else {
            ok = hasher.verify(password, user.passwordHash);
        }
        if (!ok) {
            return AuthResult.failure(AuthResult.Error.INVALID_CREDENTIALS);
        }

        if (hasher.needsRehash(user.passwordHash)) {
            try {
                users.updatePasswordHash(user.id, hasher.hash(password));
            } catch (IllegalStateException ignored) {
                // Login already succeeded against the old value. If the
                // upgrade fails we keep the old row and try again next time.
            }
        }
        return AuthResult.success(user.id, user.username);
    }

    AuthResult createAccountBlocking(String username, char[] password) {
        if (users.userExists(username)) {
            return AuthResult.failure(AuthResult.Error.USERNAME_TAKEN);
        }
        String encoded;
        try {
            encoded = hasher.hash(password);
        } catch (IllegalStateException e) {
            // Fail closed: no hash, no account. The old code would have
            // stored the plaintext here.
            return AuthResult.failure(AuthResult.Error.STORAGE_FAILED);
        }
        long id = users.insert(new UserEntity(username, encoded));
        if (id == -1) {
            // UNIQUE constraint lost the race, or the insert failed outright.
            return AuthResult.failure(users.userExists(username)
                    ? AuthResult.Error.USERNAME_TAKEN
                    : AuthResult.Error.STORAGE_FAILED);
        }
        return AuthResult.success(id, username);
    }

    static String normalizeUsername(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
