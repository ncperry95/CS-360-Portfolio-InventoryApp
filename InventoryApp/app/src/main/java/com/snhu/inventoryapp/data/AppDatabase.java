package com.snhu.inventoryapp.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * The Room database that replaces DatabaseHelper.
 *
 * Version 2 because the existing installs are at version 1, created by the
 * original SQLiteOpenHelper. exportSchema writes the schema for each version
 * to app/schemas so every schema change is versioned in the repo.
 *
 * Not used by the app yet. The migration from version 1, the triggers, and
 * the switch over from DatabaseHelper come in the next step.
 */
@Database(
        entities = {UserEntity.class, InventoryItemEntity.class},
        version = 2,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "inventory_app.db";

    public abstract UserDao userDao();

    public abstract InventoryDao inventoryDao();
}
