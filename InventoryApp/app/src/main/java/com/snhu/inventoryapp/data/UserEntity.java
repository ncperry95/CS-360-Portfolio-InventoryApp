package com.snhu.inventoryapp.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for the users table.
 *
 * Column names match the original schema so the migration from version 1
 * can copy rows straight across. The UNIQUE rule on username moves from an
 * inline column constraint to a named unique index, which is how Room
 * declares it.
 */
@Entity(
        tableName = "users",
        indices = {@Index(value = {"username"}, unique = true)})
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "username")
    public String username;

    /** Encoded hash from security.PasswordHasher, never a password. */
    @NonNull
    @ColumnInfo(name = "password_hash")
    public String passwordHash;

    public UserEntity(@NonNull String username, @NonNull String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}
