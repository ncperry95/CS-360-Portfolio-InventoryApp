package com.snhu.inventoryapp.data;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Database rules that Room cannot declare on an entity.
 *
 * Quantity can never go below zero. Room has no way to put a CHECK on an
 * entity, and its schema validation ignores CHECK, so a CHECK added only in
 * the migration would exist on upgraded installs but not on fresh ones. Two
 * triggers do the same job and are created from these exact strings in both
 * places: Callback.onCreate for a fresh install, and Migrations.MIGRATION_1_2
 * for an upgrade. Every install ends up with the same rule.
 *
 * The Callback also turns on foreign key enforcement each time the database
 * opens, so the owner_user_id foreign key is always checked.
 */
public final class SchemaRules {

    static final String TRIGGER_QUANTITY_INSERT_CHECK =
            "CREATE TRIGGER IF NOT EXISTS `inventory_quantity_insert_check` BEFORE INSERT ON `inventory` WHEN NEW.`quantity` < 0 BEGIN SELECT RAISE(ABORT, 'quantity cannot be negative'); END";

    static final String TRIGGER_QUANTITY_UPDATE_CHECK =
            "CREATE TRIGGER IF NOT EXISTS `inventory_quantity_update_check` BEFORE UPDATE OF `quantity` ON `inventory` WHEN NEW.`quantity` < 0 BEGIN SELECT RAISE(ABORT, 'quantity cannot be negative'); END";

    private SchemaRules() {
    }

    static void createQuantityTriggers(@NonNull SupportSQLiteDatabase db) {
        db.execSQL(TRIGGER_QUANTITY_INSERT_CHECK);
        db.execSQL(TRIGGER_QUANTITY_UPDATE_CHECK);
    }

    /** Added to every database the app or the tests build. */
    static final class Callback extends RoomDatabase.Callback {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            createQuantityTriggers(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("PRAGMA foreign_keys = ON");
        }
    }
}
