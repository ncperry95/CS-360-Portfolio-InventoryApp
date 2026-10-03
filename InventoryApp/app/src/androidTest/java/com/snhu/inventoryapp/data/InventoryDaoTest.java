package com.snhu.inventoryapp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The database level rules on a fresh install: the quantity triggers, the
 * owner foreign key with cascade delete, and the index on owner_user_id.
 *
 * These go straight to the DAOs, below the repository, to prove the database
 * itself enforces each rule even if a caller skipped the repository.
 */
@RunWith(AndroidJUnit4.class)
public class InventoryDaoTest {

    static final String OWNER_QUERY =
            "SELECT id, name, quantity FROM inventory WHERE owner_user_id = ? ORDER BY id ASC";

    private AppDatabase database;
    private InventoryDao inventory;
    private UserDao users;
    private long alice;
    private long bob;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.createInMemory(context);
        inventory = database.inventoryDao();
        users = database.userDao();
        alice = users.insert(new UserEntity("alice", "unused hash"));
        bob = users.insert(new UserEntity("bob", "unused hash"));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insert_negativeQuantity_isRejected() {
        assertThrows(SQLiteConstraintException.class,
                () -> inventory.insert(new InventoryItemEntity("Widget", -1, alice)));
        assertTrue(inventory.getInventoryForOwner(alice).isEmpty());
    }

    @Test
    public void update_toNegativeQuantity_isRejectedAndRowIsUnchanged() {
        long id = inventory.insert(new InventoryItemEntity("Widget", 3, alice));
        assertThrows(SQLiteConstraintException.class,
                () -> inventory.updateQuantity(id, alice, -5));
        assertEquals(3, inventory.getItem(id, alice).getQuantity());
    }

    @Test
    public void quantityOfZero_isAllowed() {
        long id = inventory.insert(new InventoryItemEntity("Widget", 0, alice));
        assertTrue(id > 0);
        assertEquals(1, inventory.updateQuantity(id, alice, 0));
    }

    @Test
    public void insert_forOwnerThatDoesNotExist_isRejectedByForeignKey() {
        assertThrows(SQLiteConstraintException.class,
                () -> inventory.insert(new InventoryItemEntity("Widget", 1, 9999L)));
    }

    @Test
    public void deletingUser_removesOnlyTheirItems() {
        inventory.insert(new InventoryItemEntity("Alice one", 1, alice));
        inventory.insert(new InventoryItemEntity("Alice two", 2, alice));
        inventory.insert(new InventoryItemEntity("Bob one", 3, bob));

        assertEquals(1, users.deleteById(alice));

        assertTrue(inventory.getInventoryForOwner(alice).isEmpty());
        assertEquals(1, inventory.getInventoryForOwner(bob).size());
    }

    @Test
    public void ownerQuery_usesTheIndexAndNeedsNoSortStep() {
        inventory.insert(new InventoryItemEntity("Widget", 1, alice));
        String plan = queryPlan(database, alice);
        Log.i("QueryPlan", "version 2 (Room): " + plan);

        assertTrue("expected an index search, got: " + plan,
                plan.contains("USING INDEX index_inventory_owner_user_id"));
        assertFalse("ORDER BY id should come straight from the index, got: " + plan,
                plan.contains("TEMP B-TREE"));
    }

    /** Joins the detail column of EXPLAIN QUERY PLAN for the owner query. */
    static String queryPlan(AppDatabase database, long ownerId) {
        StringBuilder plan = new StringBuilder();
        try (Cursor cursor = database.getOpenHelper().getReadableDatabase()
                .query("EXPLAIN QUERY PLAN " + OWNER_QUERY, new Object[]{ownerId})) {
            int detail = cursor.getColumnIndexOrThrow("detail");
            while (cursor.moveToNext()) {
                if (plan.length() > 0) {
                    plan.append(" | ");
                }
                plan.append(cursor.getString(detail));
            }
        }
        return plan.toString();
    }
}
