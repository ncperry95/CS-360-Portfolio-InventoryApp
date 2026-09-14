package com.snhu.inventoryapp;

/**
 * Plain data holder for a single inventory item.
 * Used to move item data between the database and the UI.
 */
public class InventoryItem {

    private final long id;
    private final String name;
    private final int quantity;

    public InventoryItem(long id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
