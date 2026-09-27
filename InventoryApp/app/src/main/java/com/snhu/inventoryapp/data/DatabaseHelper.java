package com.snhu.inventoryapp.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.snhu.inventoryapp.InventoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the SQLite schema and the raw row operations. Nothing else in the app
 * touches SQLiteDatabase directly.
 *
 * Changes from the original:
 *   - Password hashing moved out to security.PasswordHasher. This class stores
 *     and returns the encoded hash string and never sees a password.
 *   - Every single item operation (get, update, delete) takes the owner's user
 *     id as well as the item id and puts both in the WHERE clause. An item id
 *     that belongs to someone else matches zero rows. This closes the insecure
 *     direct object reference from the code review.
 *   - A second constructor accepts a database name so tests can pass null and
 *     get an in memory database.
 *   - The app creates exactly one instance (see di.AppContainer) instead of one
 *     per Activity.
 *
 * The schema itself, DB_VERSION, and the drop and recreate onUpgrade are left
 * as they were on purpose. Migrating to Room with versioned migrations is
 * Enhancement Three.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "inventory_app.db";
    private static final int DB_VERSION = 1;

    // Users table
    static final String TABLE_USERS = "users";
    static final String USER_ID = "id";
    static final String USER_NAME = "username";
    static final String USER_PASSWORD = "password_hash";

    // Inventory table
    static final String TABLE_INVENTORY = "inventory";
    static final String ITEM_ID = "id";
    static final String ITEM_NAME = "name";
    static final String ITEM_QUANTITY = "quantity";
    static final String ITEM_OWNER = "owner_user_id";

    /** Opens the real on disk database. */
    public DatabaseHelper(Context context) {
        this(context, DB_NAME);
    }

    /** Opens a database by name. Pass null for an in memory database (tests). */
    public DatabaseHelper(Context context, @Nullable String databaseName) {
        super(context, databaseName, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + USER_NAME + " TEXT NOT NULL UNIQUE, "
                + USER_PASSWORD + " TEXT NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_INVENTORY + " ("
                + ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ITEM_NAME + " TEXT NOT NULL, "
                + ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ITEM_OWNER + " INTEGER NOT NULL"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Unchanged from the original. Replaced by Room migrations in Enhancement Three.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // -------------------- Users --------------------

    /** A user row as stored: id plus the encoded password hash. */
    public static final class UserRecord {
        public final long id;
        public final String username;
        public final String passwordHash;

        UserRecord(long id, String username, String passwordHash) {
            this.id = id;
            this.username = username;
            this.passwordHash = passwordHash;
        }
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID},
                USER_NAME + " = ?",
                new String[]{username},
                null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Inserts a user with an already encoded password hash.
     * Returns the new row id, or -1 if the insert failed (duplicate username).
     */
    public long insertUser(String username, String passwordHash) {
        ContentValues values = new ContentValues();
        values.put(USER_NAME, username);
        values.put(USER_PASSWORD, passwordHash);
        return getWritableDatabase().insert(TABLE_USERS, null, values);
    }

    /** Looks up a user by name. Returns null when there is no such user. */
    @Nullable
    public UserRecord findUser(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID, USER_NAME, USER_PASSWORD},
                USER_NAME + " = ?",
                new String[]{username},
                null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new UserRecord(
                    cursor.getLong(cursor.getColumnIndexOrThrow(USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(USER_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD)));
        }
    }

    /**
     * Replaces a user's stored hash. Used to upgrade legacy SHA-256 rows to
     * PBKDF2 after a successful login. Returns rows changed (1 on success).
     */
    public int updatePasswordHash(long userId, String newPasswordHash) {
        ContentValues values = new ContentValues();
        values.put(USER_PASSWORD, newPasswordHash);
        return getWritableDatabase().update(
                TABLE_USERS,
                values,
                USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
    }

    // -------------------- Inventory --------------------

    /**
     * Loads every item the user owns, in row id order. Sorting for display
     * happens in memory (see domain.ItemSortOrder), so the query itself does
     * not carry an ORDER BY on name or quantity.
     */
    public List<InventoryItem> getInventoryForUser(long userId) {
        ArrayList<InventoryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{ITEM_ID, ITEM_NAME, ITEM_QUANTITY},
                ITEM_OWNER + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                ITEM_ID + " ASC")) {
            // Column lookups are resolved once, outside the loop, instead of
            // three string searches per row.
            int idIndex = cursor.getColumnIndexOrThrow(ITEM_ID);
            int nameIndex = cursor.getColumnIndexOrThrow(ITEM_NAME);
            int quantityIndex = cursor.getColumnIndexOrThrow(ITEM_QUANTITY);
            items.ensureCapacity(cursor.getCount());
            while (cursor.moveToNext()) {
                items.add(new InventoryItem(
                        cursor.getLong(idIndex),
                        cursor.getString(nameIndex),
                        cursor.getInt(quantityIndex)));
            }
        }
        return items;
    }

    /** Returns the new row id, or -1 on failure. */
    public long insertItem(long ownerId, String name, int quantity) {
        ContentValues values = new ContentValues();
        values.put(ITEM_NAME, name);
        values.put(ITEM_QUANTITY, quantity);
        values.put(ITEM_OWNER, ownerId);
        return getWritableDatabase().insert(TABLE_INVENTORY, null, values);
    }

    /**
     * Updates quantity only when the row exists AND belongs to ownerId.
     * Returns rows changed: 1 on success, 0 if the item is missing or is
     * someone else's.
     */
    public int updateQuantity(long itemId, long ownerId, int newQuantity) {
        ContentValues values = new ContentValues();
        values.put(ITEM_QUANTITY, newQuantity);
        return getWritableDatabase().update(
                TABLE_INVENTORY,
                values,
                ITEM_ID + " = ? AND " + ITEM_OWNER + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(ownerId)});
    }

    /** Deletes only when the row belongs to ownerId. Returns rows deleted. */
    public int deleteItem(long itemId, long ownerId) {
        return getWritableDatabase().delete(
                TABLE_INVENTORY,
                ITEM_ID + " = ? AND " + ITEM_OWNER + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(ownerId)});
    }

    /** Returns the item only when it belongs to ownerId, otherwise null. */
    @Nullable
    public InventoryItem getItem(long itemId, long ownerId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{ITEM_ID, ITEM_NAME, ITEM_QUANTITY},
                ITEM_ID + " = ? AND " + ITEM_OWNER + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(ownerId)},
                null, null, null)) {
            return cursor.moveToFirst() ? readItem(cursor) : null;
        }
    }

    private static InventoryItem readItem(Cursor cursor) {
        return new InventoryItem(
                cursor.getLong(cursor.getColumnIndexOrThrow(ITEM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(ITEM_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_QUANTITY)));
    }
}
