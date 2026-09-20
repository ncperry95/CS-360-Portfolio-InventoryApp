package com.snhu.inventoryapp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snhu.inventoryapp.InventoryItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs on a device or emulator against an in memory SQLite database.
 *
 * The executor is Runnable::run so every repository call completes before
 * the assertion. The point of these tests is the authorization boundary:
 * user A must not be able to read, change, or delete user B's item even when
 * A knows B's item id.
 */
@RunWith(AndroidJUnit4.class)
public class InventoryRepositoryTest {

    private static final long ALICE = 1L;
    private static final long BOB = 2L;

    private DatabaseHelper db;
    private InventoryRepository repo;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = new DatabaseHelper(context, null); // null name = in memory
        repo = new InventoryRepository(db, Runnable::run);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void addAndLoad_returnsOnlyOwnersItems() {
        long a = add(ALICE, "Alice widget", 3);
        long b = add(BOB, "Bob gadget", 7);
        assertTrue(a > 0 && b > 0);

        List<InventoryItem> alice = load(ALICE);
        assertEquals(1, alice.size());
        assertEquals("Alice widget", alice.get(0).getName());

        List<InventoryItem> bob = load(BOB);
        assertEquals(1, bob.size());
        assertEquals("Bob gadget", bob.get(0).getName());
    }

    @Test
    public void getItem_ownerSeesIt_otherUserGetsNull() {
        long id = add(ALICE, "Alice widget", 3);

        InventoryItem asAlice = get(id, ALICE);
        assertNotNull(asAlice);
        assertEquals(id, asAlice.getId());

        assertNull("Bob must not read Alice's item by id", get(id, BOB));
    }

    @Test
    public void updateQuantity_otherUserChangesNothing() {
        long id = add(ALICE, "Alice widget", 3);

        assertFalse("Bob's update must affect zero rows", update(id, BOB, 99));
        assertEquals(3, get(id, ALICE).getQuantity());

        assertTrue(update(id, ALICE, 4));
        assertEquals(4, get(id, ALICE).getQuantity());
    }

    @Test
    public void deleteItem_otherUserDeletesNothing() {
        long id = add(ALICE, "Alice widget", 3);

        assertFalse("Bob's delete must affect zero rows", delete(id, BOB));
        assertNotNull(get(id, ALICE));

        assertTrue(delete(id, ALICE));
        assertNull(get(id, ALICE));
        assertTrue(load(ALICE).isEmpty());
    }

    @Test
    public void update_onMissingItem_reportsFalse() {
        assertFalse(update(12345L, ALICE, 1));
        assertFalse(delete(12345L, ALICE));
        assertNull(get(12345L, ALICE));
    }

    // -------------------- helpers --------------------

    private long add(long owner, String name, int qty) {
        AtomicReference<Long> out = new AtomicReference<>();
        repo.addItem(owner, name, qty, out::set);
        return out.get();
    }

    private List<InventoryItem> load(long owner) {
        AtomicReference<List<InventoryItem>> out = new AtomicReference<>();
        repo.loadInventory(owner, out::set);
        return out.get();
    }

    private InventoryItem get(long id, long owner) {
        AtomicReference<InventoryItem> out = new AtomicReference<>();
        repo.getItem(id, owner, out::set);
        return out.get();
    }

    private boolean update(long id, long owner, int qty) {
        AtomicReference<Boolean> out = new AtomicReference<>();
        repo.updateQuantity(id, owner, qty, out::set);
        return out.get();
    }

    private boolean delete(long id, long owner) {
        AtomicReference<Boolean> out = new AtomicReference<>();
        repo.deleteItem(id, owner, out::set);
        return out.get();
    }
}
