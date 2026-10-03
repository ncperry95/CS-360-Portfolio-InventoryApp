package com.snhu.inventoryapp.data;

import android.database.SQLException;

import com.snhu.inventoryapp.InventoryItem;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Single entry point for inventory data. Activities and ViewModels never see
 * Room or the DAO.
 *
 * Every operation takes the owner's user id and passes it down to the WHERE
 * clause, so the authorization check cannot be skipped by a caller that
 * forgets it. Every write reports whether it actually changed a row, and the
 * ViewModel turns a zero into a visible message instead of silence.
 *
 * The database itself now enforces two rules as well: an item must belong to
 * a user who exists (foreign key), and quantity can never go below zero
 * (trigger). When a write breaks one of them SQLite throws, and this class
 * turns that into the same failed result the ViewModel already handles.
 *
 * All work runs on the injected Executor. The app uses a single thread
 * executor so SQLite writes are serialized without extra locking; tests use a
 * direct executor so calls complete before the assertion runs.
 */
public class InventoryRepository {

    private final InventoryDao dao;
    private final Executor executor;

    public InventoryRepository(InventoryDao dao, Executor executor) {
        this.dao = dao;
        this.executor = executor;
    }

    public void loadInventory(long ownerId, RepositoryCallback<List<InventoryItem>> callback) {
        executor.execute(() -> callback.onResult(dao.getInventoryForOwner(ownerId)));
    }

    /** Result is the new item id, or -1 if the insert failed. */
    public void addItem(long ownerId, String name, int quantity, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            long id;
            try {
                id = dao.insert(new InventoryItemEntity(name, quantity, ownerId));
            } catch (SQLException e) {
                // Negative quantity, an owner that does not exist, or a
                // storage failure. Same -1 contract as before.
                id = -1;
            }
            callback.onResult(id);
        });
    }

    /** Result is true only when exactly one row owned by ownerId was updated. */
    public void updateQuantity(long itemId, long ownerId, int newQuantity,
                               RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            boolean updated;
            try {
                updated = dao.updateQuantity(itemId, ownerId, newQuantity) == 1;
            } catch (SQLException e) {
                // The quantity trigger rejected a negative value.
                updated = false;
            }
            callback.onResult(updated);
        });
    }

    /** Result is true only when exactly one row owned by ownerId was deleted. */
    public void deleteItem(long itemId, long ownerId, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> callback.onResult(dao.delete(itemId, ownerId) == 1));
    }

    /** Result is the item, or null when it does not exist or is not ownerId's. */
    public void getItem(long itemId, long ownerId, RepositoryCallback<InventoryItem> callback) {
        executor.execute(() -> callback.onResult(dao.getItem(itemId, ownerId)));
    }
}
