package com.snhu.inventoryapp;

import android.content.Context;

import com.snhu.inventoryapp.auth.SessionManager;
import com.snhu.inventoryapp.data.AppDatabase;
import com.snhu.inventoryapp.data.InventoryRepository;
import com.snhu.inventoryapp.data.UserRepository;
import com.snhu.inventoryapp.security.PasswordHasher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manual dependency container. Builds the app's shared objects once and
 * hands them to ViewModels through {@link ViewModelFactory}.
 *
 * This replaces the original pattern where each Activity created its own
 * DatabaseHelper and never closed it. Now there is one Room database, one
 * executor, one session, and one instance of each repository for the whole
 * process. Opening the database the first time after an upgrade runs the
 * migration from the original schema.
 */
public class AppContainer {

    /**
     * A single thread so database work is serialized. SQLite allows one
     * writer at a time anyway; a bigger pool would just queue on its lock.
     */
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private final AppDatabase database;
    private final SessionManager sessionManager = new SessionManager();
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    public AppContainer(Context appContext) {
        database = AppDatabase.create(appContext.getApplicationContext());
        userRepository = new UserRepository(database.userDao(), new PasswordHasher(), databaseExecutor);
        inventoryRepository = new InventoryRepository(database.inventoryDao(), databaseExecutor);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public InventoryRepository getInventoryRepository() {
        return inventoryRepository;
    }
}
