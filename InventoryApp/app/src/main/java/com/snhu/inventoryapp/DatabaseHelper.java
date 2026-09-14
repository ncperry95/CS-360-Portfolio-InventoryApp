package com.snhu.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQLite database work for the Inventory App.
 *
 * Two tables:
 *   users     - holds login info (username + hashed password)
 *   inventory - holds items, each tied to the user who created them
 *
 * The class exposes simple helper methods so the activities never have to
 * touch raw SQL.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database file name and version. Bump the version if the schema ever changes.
    private static final String DB_NAME = "inventory_app.db";
    private static final int DB_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String USER_ID = "id";
    private static final String USER_NAME = "username";
    private static final String USER_PASSWORD = "password_hash";

    // Inventory table
    private static final String TABLE_INVENTORY = "inventory";
    private static final String ITEM_ID = "id";
    private static final String ITEM_NAME = "name";
    private static final String ITEM_QUANTITY = "quantity";
    private static final String ITEM_OWNER = "owner_user_id";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the users table. Username is unique so two people cannot share one.
        String createUsers =
                "CREATE TABLE " + TABLE_USERS + " ("
                        + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + USER_NAME + " TEXT NOT NULL UNIQUE, "
                        + USER_PASSWORD + " TEXT NOT NULL"
                        + ")";
        db.execSQL(createUsers);

        // Create the inventory table. Each item is tied to the user who added it.
        String createInventory =
                "CREATE TABLE " + TABLE_INVENTORY + " ("
                        + ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + ITEM_NAME + " TEXT NOT NULL, "
                        + ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                        + ITEM_OWNER + " INTEGER NOT NULL"
                        + ")";
        db.execSQL(createInventory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple upgrade strategy: wipe and rebuild. Fine for a class project.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // -------------------- User methods --------------------

    /**
     * Returns true if the username already exists in the database.
     */
    public boolean userExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID},
                USER_NAME + " = ?",
                new String[]{username},
                null, null, null);
        boolean found = cursor.moveToFirst();
        cursor.close();
        return found;
    }

    /**
     * Inserts a new user with a hashed password.
     * Returns the new user's id, or -1 if the insert failed
     * (for example, if the username was already taken).
     */
    public long createUser(String username, String password) {
        ContentValues values = new ContentValues();
        values.put(USER_NAME, username);
        values.put(USER_PASSWORD, hashPassword(password));
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE_USERS, null, values);
    }

    /**
     * Checks the given username and password against the database.
     * Returns the user's id on a match, or -1 if the credentials are wrong.
     */
    public long checkLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID, USER_PASSWORD},
                USER_NAME + " = ?",
                new String[]{username},
                null, null, null);

        long userId = -1;
        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD));
            if (storedHash.equals(hashPassword(password))) {
                userId = cursor.getLong(cursor.getColumnIndexOrThrow(USER_ID));
            }
        }
        cursor.close();
        return userId;
    }

    // -------------------- Inventory methods --------------------

    /**
     * Returns every item belonging to the given user, in the order they were added.
     */
    public List<InventoryItem> getInventoryForUser(long userId) {
        List<InventoryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{ITEM_ID, ITEM_NAME, ITEM_QUANTITY},
                ITEM_OWNER + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                ITEM_ID + " ASC");

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(ITEM_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_NAME));
            int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_QUANTITY));
            items.add(new InventoryItem(id, name, quantity));
        }
        cursor.close();
        return items;
    }

    /**
     * Adds a new inventory item for the given user.
     * Returns the new item's id, or -1 if the insert failed.
     */
    public long addItem(long userId, String name, int quantity) {
        ContentValues values = new ContentValues();
        values.put(ITEM_NAME, name);
        values.put(ITEM_QUANTITY, quantity);
        values.put(ITEM_OWNER, userId);
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE_INVENTORY, null, values);
    }

    /**
     * Updates the quantity of an existing item.
     * Returns the number of rows changed, which should be 1 on success.
     */
    public int updateQuantity(long itemId, int newQuantity) {
        ContentValues values = new ContentValues();
        values.put(ITEM_QUANTITY, newQuantity);
        SQLiteDatabase db = getWritableDatabase();
        return db.update(
                TABLE_INVENTORY,
                values,
                ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)});
    }

    /**
     * Deletes an item by its id.
     * Returns the number of rows deleted, which should be 1 on success.
     */
    public int deleteItem(long itemId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(
                TABLE_INVENTORY,
                ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)});
    }

    /**
     * Looks up a single item by its id. Returns null if not found.
     * Used by the item detail screen so it can show fresh data.
     */
    public InventoryItem getItem(long itemId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{ITEM_ID, ITEM_NAME, ITEM_QUANTITY},
                ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)},
                null, null, null);

        InventoryItem item = null;
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(ITEM_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_NAME));
            int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_QUANTITY));
            item = new InventoryItem(id, name, quantity);
        }
        cursor.close();
        return item;
    }

    // -------------------- Helpers --------------------

    /**
     * Hashes a password with SHA-256 so we never store the plain text in the database.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // SHA-256 and UTF-8 are always available on Android, but the compiler
            // requires us to handle the exceptions. If something goes truly wrong,
            // fall back to the raw password so the app does not crash.
            return password;
        }
    }
}