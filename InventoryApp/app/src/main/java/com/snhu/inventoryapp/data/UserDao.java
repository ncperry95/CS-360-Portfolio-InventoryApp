package com.snhu.inventoryapp.data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Account queries. Room checks every query here against the schema when the
 * app compiles, so a typo in a column name is a build error instead of a
 * crash at runtime.
 */
@Dao
public interface UserDao {

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    boolean userExists(String username);

    /**
     * Returns the new row id, or -1 when the username is already taken.
     * IGNORE keeps the same contract the old insertUser had, so
     * UserRepository can tell a duplicate apart from a success.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserEntity user);

    @Nullable
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity findByUsername(String username);

    /** Used to upgrade a legacy SHA 256 row to PBKDF2. Returns rows changed. */
    @Query("UPDATE users SET password_hash = :passwordHash WHERE id = :userId")
    int updatePasswordHash(long userId, String passwordHash);

    /** Returns rows deleted. The foreign key removes the user's items too. */
    @Query("DELETE FROM users WHERE id = :userId")
    int deleteById(long userId);
}
