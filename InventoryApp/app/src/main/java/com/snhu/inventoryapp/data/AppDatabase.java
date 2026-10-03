package com.snhu.inventoryapp.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The Room database that replaces DatabaseHelper.
 *
 * Version 2 because the existing installs are at version 1, created by the
 * original SQLiteOpenHelper. exportSchema writes the schema for each version
 * to app/schemas so every schema change is versioned in the repo.
 *
 * There is deliberately no fallbackToDestructiveMigration. If a future
 * version ships without a migration, the app fails loudly instead of quietly
 * deleting everyone's inventory the way the original onUpgrade did.
 */
@Database(
        entities = {UserEntity.class, InventoryItemEntity.class},
        version = 2,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "inventory_app.db";

    public abstract UserDao userDao();

    public abstract InventoryDao inventoryDao();

    /** The app's on disk database. AppContainer calls this exactly once. */
    @NonNull
    public static AppDatabase create(@NonNull Context context) {
        return create(context, DATABASE_NAME);
    }

    /** Opens a database file by name. Tests use this to run the migration. */
    @NonNull
    static AppDatabase create(@NonNull Context context, @NonNull String name) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, name)
                .addMigrations(Migrations.MIGRATION_1_2)
                .addCallback(new SchemaRules.Callback())
                .build();
    }

    /** An in memory database with the same rules as the real one. Tests only. */
    @NonNull
    static AppDatabase createInMemory(@NonNull Context context) {
        return Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                .addCallback(new SchemaRules.Callback())
                .build();
    }
}
