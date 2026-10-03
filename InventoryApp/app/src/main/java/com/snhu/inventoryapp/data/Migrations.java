package com.snhu.inventoryapp.data;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Schema migrations. Replaces the original onUpgrade, which dropped both
 * tables and would have wiped every user's data on the first schema change.
 *
 * Version 1 is the schema the original SQLiteOpenHelper created. Version 2 is
 * the Room schema in app/schemas. The steps:
 *   1. Copy users, inventory, and the AUTOINCREMENT counters into temp tables.
 *   2. Drop the old tables, child first.
 *   3. Create the new tables with the exact SQL Room exported, so Room's own
 *      schema check after the migration passes.
 *   4. Copy the rows back with their ids and password hashes unchanged.
 *      Items whose owner no longer exists are left out, since nobody could
 *      ever reach them and the foreign key would reject them. A negative
 *      quantity, which the old schema allowed, is stored as zero.
 *   5. Add the indexes, restore the AUTOINCREMENT counters so a deleted id is
 *      never handed out again, drop the temp tables, and add the triggers.
 *
 * No table is renamed. Rename behavior differs between the SQLite versions
 * shipped on API 26 through 29 and newer ones, and creating the tables under
 * their final names avoids depending on it. Room runs a migration inside one
 * transaction, so if any step fails the version 1 data is left as it was.
 */
public final class Migrations {

    static final String[] MIGRATION_1_2_STATEMENTS = {
            "CREATE TEMP TABLE `users_backup` AS SELECT `id`, `username`, `password_hash` FROM `users`",
            "CREATE TEMP TABLE `inventory_backup` AS SELECT `id`, `name`, `quantity`, `owner_user_id` FROM `inventory`",
            "CREATE TEMP TABLE `sequence_backup` AS SELECT `name`, `seq` FROM `sqlite_sequence` WHERE `name` IN ('users', 'inventory')",
            "DROP TABLE `inventory`",
            "DROP TABLE `users`",
            "CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `password_hash` TEXT NOT NULL)",
            "INSERT INTO `users` (`id`, `username`, `password_hash`) SELECT `id`, `username`, `password_hash` FROM `users_backup`",
            "CREATE TABLE IF NOT EXISTS `inventory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `quantity` INTEGER NOT NULL DEFAULT 0, `owner_user_id` INTEGER NOT NULL, FOREIGN KEY(`owner_user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "INSERT INTO `inventory` (`id`, `name`, `quantity`, `owner_user_id`) SELECT `id`, `name`, MAX(`quantity`, 0), `owner_user_id` FROM `inventory_backup` WHERE `owner_user_id` IN (SELECT `id` FROM `users`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)",
            "CREATE INDEX IF NOT EXISTS `index_inventory_owner_user_id` ON `inventory` (`owner_user_id`)",
            "UPDATE `sqlite_sequence` SET `seq` = (SELECT b.`seq` FROM `sequence_backup` b WHERE b.`name` = `sqlite_sequence`.`name`) WHERE `seq` < (SELECT b.`seq` FROM `sequence_backup` b WHERE b.`name` = `sqlite_sequence`.`name`)",
            "INSERT INTO `sqlite_sequence` (`name`, `seq`) SELECT b.`name`, b.`seq` FROM `sequence_backup` b WHERE NOT EXISTS (SELECT 1 FROM `sqlite_sequence` s WHERE s.`name` = b.`name`)",
            "DROP TABLE `users_backup`",
            "DROP TABLE `inventory_backup`",
            "DROP TABLE `sequence_backup`"
    };

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            for (String sql : MIGRATION_1_2_STATEMENTS) {
                db.execSQL(sql);
            }
            SchemaRules.createQuantityTriggers(db);
        }
    };

    private Migrations() {
    }
}
