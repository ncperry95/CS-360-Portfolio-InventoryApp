package com.snhu.inventoryapp.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for the inventory table.
 *
 * Two things the original schema did not have:
 *   A foreign key from owner_user_id to users.id. An item can no longer point
 *   at a user that does not exist, and deleting a user removes their items.
 *   An index on owner_user_id. Every inventory query filters on the owner, so
 *   without the index each load scans the whole table.
 *
 * The rule that quantity can never go below zero is not declared here. Room
 * has no way to put a CHECK on an entity, so it lives in database triggers
 * that are created for both fresh installs and upgraded ones.
 */
@Entity(
        tableName = "inventory",
        foreignKeys = {@ForeignKey(
                entity = UserEntity.class,
                parentColumns = {"id"},
                childColumns = {"owner_user_id"},
                onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"owner_user_id"})})
public class InventoryItemEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "quantity", defaultValue = "0")
    public int quantity;

    @ColumnInfo(name = "owner_user_id")
    public long ownerUserId;

    public InventoryItemEntity(@NonNull String name, int quantity, long ownerUserId) {
        this.name = name;
        this.quantity = quantity;
        this.ownerUserId = ownerUserId;
    }
}
