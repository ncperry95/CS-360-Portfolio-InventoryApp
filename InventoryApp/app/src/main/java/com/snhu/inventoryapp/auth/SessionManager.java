package com.snhu.inventoryapp.auth;

/**
 * Holds the currently authenticated user for the life of the process.
 *
 * The original app passed the user id from screen to screen as an Intent
 * extra, which meant there was no single place that knew who was logged in,
 * nothing to invalidate on logout, and any screen could be started with any
 * id. Now the login screen calls {@link #login}, every other screen asks
 * {@link #getUserId}, and logout clears it.
 *
 * State lives in memory only. Closing the app logs the user out, which is the
 * right default for an app that has no server side session to resume.
 */
public class SessionManager {

    /** Value returned by {@link #getUserId} when nobody is logged in. */
    public static final long NO_USER = -1L;

    private long userId = NO_USER;
    private String username;

    public synchronized void login(long userId, String username) {
        if (userId < 0) {
            throw new IllegalArgumentException("userId must be a real row id");
        }
        this.userId = userId;
        this.username = username;
    }

    public synchronized void logout() {
        userId = NO_USER;
        username = null;
    }

    public synchronized boolean isLoggedIn() {
        return userId != NO_USER;
    }

    public synchronized long getUserId() {
        return userId;
    }

    public synchronized String getUsername() {
        return username;
    }
}
