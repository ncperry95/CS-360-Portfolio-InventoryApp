package com.snhu.inventoryapp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.auth.AuthResult;
import com.snhu.inventoryapp.security.PasswordHasher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Upgrades a real version 1 database file to version 2 and checks that
 * nothing was lost.
 *
 * Room's usual MigrationTestHelper needs an exported schema for version 1,
 * and there is none because version 1 was never a Room database. So setUp
 * builds version 1 the way the original app did: a plain SQLiteOpenHelper
 * running the original CREATE TABLE statements. Opening it through
 * AppDatabase then runs MIGRATION_1_2, and Room checks the result against
 * the version 2 schema before any test code runs. A mismatch fails setUp.
 *
 * The version 1 data is deliberately messy: a legacy SHA 256 password, an
 * item whose owner does not exist, a negative quantity, and a deleted item
 * so the AUTOINCREMENT counter is ahead of the highest remaining id.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String DB_NAME = "migration_test.db";

    /** SHA 256 of "password", the way the original app stored it. */
    private static final String LEGACY_HASH =
            "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

    private static final String MODERN_PASSWORD = "correct horse 1";

    private static final long LEGACY_USER = 1L;
    private static final long MODERN_USER = 2L;

    private Context context;
    private String modernHash;
    private String versionOnePlan;
    private AppDatabase database;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DB_NAME);
        modernHash = new PasswordHasher().hash(MODERN_PASSWORD.toCharArray());
        createVersionOneDatabase();

        database = AppDatabase.create(context, DB_NAME);
        // Opening the database is what runs the migration and Room's check.
        database.getOpenHelper().getWritableDatabase();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void migration_setsVersionTwo() {
        assertEquals(2, readInt("PRAGMA user_version"));
    }

    @Test
    public void migration_keepsUsersIdsAndHashesExactly() {
        UserEntity legacy = database.userDao().findByUsername("legacyuser");
        assertNotNull(legacy);
        assertEquals(LEGACY_USER, legacy.id);
        assertEquals(LEGACY_HASH, legacy.passwordHash);

        UserEntity modern = database.userDao().findByUsername("modernuser");
        assertNotNull(modern);
        assertEquals(MODERN_USER, modern.id);
        assertEquals(modernHash, modern.passwordHash);
    }

    @Test
    public void migration_keepsItems_dropsOrphan_clampsNegativeToZero() {
        List<InventoryItem> legacyItems = database.inventoryDao().getInventoryForOwner(LEGACY_USER);
        assertEquals(3, legacyItems.size());
        assertEquals(new InventoryItem(1, "Widget", 3), legacyItems.get(0));
        assertEquals(new InventoryItem(2, "Gadget", 0), legacyItems.get(1));
        assertEquals(new InventoryItem(5, "Broken", 0), legacyItems.get(2));

        List<InventoryItem> modernItems = database.inventoryDao().getInventoryForOwner(MODERN_USER);
        assertEquals(1, modernItems.size());
        assertEquals(new InventoryItem(3, "Bolt", 12), modernItems.get(0));

        assertEquals("orphan item 4 must not survive", 0,
                readInt("SELECT COUNT(*) FROM inventory WHERE id = 4"));
        assertEquals(4, readInt("SELECT COUNT(*) FROM inventory"));
    }

    @Test
    public void migration_keepsAutoincrementCounter_soDeletedIdsAreNotReused() {
        // Item 6 existed in version 1 and was deleted. The next id must be 7.
        long next = database.inventoryDao().insert(new InventoryItemEntity("New", 1, LEGACY_USER));
        assertEquals(7L, next);
    }

    @Test
    public void migration_addsRulesThatHoldAfterwards() {
        InventoryDao inventory = database.inventoryDao();
        assertThrows(SQLiteConstraintException.class,
                () -> inventory.insert(new InventoryItemEntity("Bad", -1, LEGACY_USER)));
        assertThrows(SQLiteConstraintException.class,
                () -> inventory.updateQuantity(1, LEGACY_USER, -5));
        assertThrows(SQLiteConstraintException.class,
                () -> inventory.insert(new InventoryItemEntity("Ghost", 1, 99L)));
        assertEquals(-1L, database.userDao().insert(new UserEntity("legacyuser", "duplicate")));
    }

    @Test
    public void migration_replacesFullTableScanWithIndexSearch() {
        String versionTwoPlan = InventoryDaoTest.queryPlan(database, LEGACY_USER);
        Log.i("QueryPlan", "version 1 (original): " + versionOnePlan);
        Log.i("QueryPlan", "version 2 (migrated): " + versionTwoPlan);

        assertTrue("version 1 should scan the table, got: " + versionOnePlan,
                versionOnePlan.contains("SCAN"));
        assertTrue("version 2 should use the index, got: " + versionTwoPlan,
                versionTwoPlan.contains("USING INDEX index_inventory_owner_user_id"));
        assertFalse(versionTwoPlan.contains("TEMP B-TREE"));
    }

    @Test
    public void migratedLegacyUser_logsInOnceAndIsUpgradedToPbkdf2() {
        UserRepository repo =
                new UserRepository(database.userDao(), new PasswordHasher(), Runnable::run);

        assertTrue(login(repo, "legacyuser", "password").isSuccess());
        String upgraded = database.userDao().findByUsername("legacyuser").passwordHash;
        assertFalse(PasswordHasher.isLegacyHash(upgraded));
        assertTrue(upgraded.startsWith("pbkdf2_sha256$"));

        assertTrue(login(repo, "modernuser", MODERN_PASSWORD).isSuccess());
        assertFalse(login(repo, "modernuser", "wrong password 1").isSuccess());
    }

    // -------------------- helpers --------------------

    /** Builds the version 1 file exactly as the original DatabaseHelper did. */
    private void createVersionOneDatabase() {
        SQLiteOpenHelper original = new SQLiteOpenHelper(context, DB_NAME, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL)");
                db.execSQL("CREATE TABLE inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, quantity INTEGER NOT NULL DEFAULT 0, owner_user_id INTEGER NOT NULL)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        };
        SQLiteDatabase db = original.getWritableDatabase();
        db.execSQL("INSERT INTO users (id, username, password_hash) VALUES (?, ?, ?)",
                new Object[]{LEGACY_USER, "legacyuser", LEGACY_HASH});
        db.execSQL("INSERT INTO users (id, username, password_hash) VALUES (?, ?, ?)",
                new Object[]{MODERN_USER, "modernuser", modernHash});
        insertItem(db, 1, "Widget", 3, LEGACY_USER);
        insertItem(db, 2, "Gadget", 0, LEGACY_USER);
        insertItem(db, 3, "Bolt", 12, MODERN_USER);
        insertItem(db, 4, "Ghost", 5, 99L);        // owner does not exist
        insertItem(db, 5, "Broken", -4, LEGACY_USER); // the old schema allowed this
        insertItem(db, 6, "Temp", 1, LEGACY_USER);
        db.execSQL("DELETE FROM inventory WHERE id = 6");

        StringBuilder plan = new StringBuilder();
        try (Cursor cursor = db.rawQuery("EXPLAIN QUERY PLAN " + InventoryDaoTest.OWNER_QUERY,
                new String[]{String.valueOf(LEGACY_USER)})) {
            int detail = cursor.getColumnIndexOrThrow("detail");
            while (cursor.moveToNext()) {
                if (plan.length() > 0) {
                    plan.append(" | ");
                }
                plan.append(cursor.getString(detail));
            }
        }
        versionOnePlan = plan.toString();
        original.close();
    }

    private static void insertItem(SQLiteDatabase db, long id, String name, int quantity, long owner) {
        db.execSQL("INSERT INTO inventory (id, name, quantity, owner_user_id) VALUES (?, ?, ?, ?)",
                new Object[]{id, name, quantity, owner});
    }

    private int readInt(String sql) {
        SupportSQLiteDatabase db = database.getOpenHelper().getReadableDatabase();
        try (Cursor cursor = db.query(sql)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private static AuthResult login(UserRepository repo, String user, String pass) {
        AtomicReference<AuthResult> out = new AtomicReference<>();
        repo.login(user, pass.toCharArray(), out::set);
        return out.get();
    }
}
