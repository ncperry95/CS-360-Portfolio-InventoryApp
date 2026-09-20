package com.snhu.inventoryapp.data;

import com.snhu.inventoryapp.InventoryItem;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Single entry point for inventory data. Activities and ViewModels never see
 * DatabaseHelper.
 *
 * Every operation takes the owner's user id and passes it down to the WHERE
 * clause, so the authorization check cannot be skipped by a caller that
 * forgets it. Every write reports whether it actually changed a row, and the
 * ViewModel turns a zero into a visible message instead of silence.
 *
 * All work runs on the injected Executor. The app uses a single thread
 * executor so SQLite writes are serialized without extra locking; tests use a
 * direct executor so calls complete before the assertion runs.
 */
public class InventoryRepository {

    private final DatabaseHelper db;
    private final Executor executor;

    public InventoryRepository(DatabaseHelper db, Executor executor) {
        this.db = db;
        this.executor = executor;
    }

    public void loadInventory(long ownerId, RepositoryCallback<List<InventoryItem>> callback) {
        executor.execute(() -> callback.onResult(db.getInventoryForUser(ownerId)));
    }

    /** Result is the new item id, or -1 if the insert failed. */
    public void addItem(long ownerId, String name, int quantity, RepositoryCallback<Long> callback) {
        executor.execute(() -> callback.onResult(db.insertItem(ownerId, name, quantity)));
    }

    /** Result is true only when exactly one row owned by ownerId was updated. */
    public void updateQuantity(long itemId, long ownerId, int newQuantity,
                               RepositoryCallback<Boolean> callback) {
        executor.execute(() -> callback.onResult(db.updateQuantity(itemId, ownerId, newQuantity) == 1));
    }

    /** Result is true only when exactly one row owned by ownerId was deleted. */
    public void deleteItem(long itemId, long ownerId, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> callback.onResult(db.deleteItem(itemId, ownerId) == 1));
    }

    /** Result is the item, or null when it does not exist or is not ownerId's. */
    public void getItem(long itemId, long ownerId, RepositoryCallback<InventoryItem> callback) {
        executor.execute(() -> callback.onResult(db.getItem(itemId, ownerId)));
    }
}
