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
 * Runs on a device or emulator against an in memory Room database built with
 * the same rules as the real one.
 *
 * The executor is Runnable::run so every repository call completes before
 * the assertion. The point of these tests is the authorization boundary:
 * user A must not be able to read, change, or delete user B's item even when
 * A knows B's item id.
 *
 * The foreign key on owner_user_id means items can only belong to users who
 * exist, so setUp creates Alice and Bob before any item is added.
 */
@RunWith(AndroidJUnit4.class)
public class InventoryRepositoryTest {

    private AppDatabase database;
    private InventoryRepository repo;
    private long aliceId;
    private long bobId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.createInMemory(context);
        repo = new InventoryRepository(database.inventoryDao(), Runnable::run);
        aliceId = database.userDao().insert(new UserEntity("alice", "unused hash"));
        bobId = database.userDao().insert(new UserEntity("bob", "unused hash"));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void addAndLoad_returnsOnlyOwnersItems() {
        long a = add(aliceId, "Alice widget", 3);
        long b = add(bobId, "Bob gadget", 7);
        assertTrue(a > 0 && b > 0);

        List<InventoryItem> alice = load(aliceId);
        assertEquals(1, alice.size());
        assertEquals("Alice widget", alice.get(0).getName());

        List<InventoryItem> bob = load(bobId);
        assertEquals(1, bob.size());
        assertEquals("Bob gadget", bob.get(0).getName());
    }

    @Test
    public void getItem_ownerSeesIt_otherUserGetsNull() {
        long id = add(aliceId, "Alice widget", 3);

        InventoryItem asAlice = get(id, aliceId);
        assertNotNull(asAlice);
        assertEquals(id, asAlice.getId());

        assertNull("Bob must not read Alice's item by id", get(id, bobId));
    }

    @Test
    public void updateQuantity_otherUserChangesNothing() {
        long id = add(aliceId, "Alice widget", 3);

        assertFalse("Bob's update must affect zero rows", update(id, bobId, 99));
        assertEquals(3, get(id, aliceId).getQuantity());

        assertTrue(update(id, aliceId, 4));
        assertEquals(4, get(id, aliceId).getQuantity());
    }

    @Test
    public void deleteItem_otherUserDeletesNothing() {
        long id = add(aliceId, "Alice widget", 3);

        assertFalse("Bob's delete must affect zero rows", delete(id, bobId));
        assertNotNull(get(id, aliceId));

        assertTrue(delete(id, aliceId));
        assertNull(get(id, aliceId));
        assertTrue(load(aliceId).isEmpty());
    }

    @Test
    public void update_onMissingItem_reportsFalse() {
        assertFalse(update(12345L, aliceId, 1));
        assertFalse(delete(12345L, aliceId));
        assertNull(get(12345L, aliceId));
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
