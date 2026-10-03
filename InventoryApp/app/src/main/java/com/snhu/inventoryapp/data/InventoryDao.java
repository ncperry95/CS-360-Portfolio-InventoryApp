package com.snhu.inventoryapp.data;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.snhu.inventoryapp.InventoryItem;

import java.util.List;

/**
 * Inventory queries. Every query that touches a single item takes the owner's
 * user id along with the item id, so an item id belonging to someone else
 * matches zero rows. That is the access control fix from the code review,
 * carried over from DatabaseHelper and now checked by Room at compile time.
 *
 * Reads return the existing InventoryItem class directly, so nothing above
 * the repository has to change.
 */
@Dao
public interface InventoryDao {

    /**
     * Every item the owner has, in row id order. Display sorting happens in
     * memory (domain.ItemSortOrder). The index on owner_user_id also stores
     * the row id, so this query needs no separate sort step.
     */
    @Query("SELECT id, name, quantity FROM inventory WHERE owner_user_id = :ownerId ORDER BY id ASC")
    List<InventoryItem> getInventoryForOwner(long ownerId);

    /**
     * Returns the new row id. Throws if the owner does not exist (foreign
     * key) or the quantity is below zero (trigger); InventoryRepository
     * turns that into a failed result.
     */
    @Insert
    long insert(InventoryItemEntity item);

    /** Returns rows changed: 1 on success, 0 if missing or not ownerId's. */
    @Query("UPDATE inventory SET quantity = :newQuantity WHERE id = :itemId AND owner_user_id = :ownerId")
    int updateQuantity(long itemId, long ownerId, int newQuantity);

    /** Returns rows deleted: 1 on success, 0 if missing or not ownerId's. */
    @Query("DELETE FROM inventory WHERE id = :itemId AND owner_user_id = :ownerId")
    int delete(long itemId, long ownerId);

    /** Returns the item only when it belongs to ownerId, otherwise null. */
    @Nullable
    @Query("SELECT id, name, quantity FROM inventory WHERE id = :itemId AND owner_user_id = :ownerId")
    InventoryItem getItem(long itemId, long ownerId);
}
