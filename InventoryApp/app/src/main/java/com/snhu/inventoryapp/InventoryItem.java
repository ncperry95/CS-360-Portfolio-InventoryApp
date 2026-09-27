package com.snhu.inventoryapp;

/**
 * Plain data holder for a single inventory item.
 * Used to move item data between the database and the UI.
 *
 * Value equality is defined over all three fields so that DiffUtil can tell
 * "same row, changed quantity" apart from "same row, nothing changed", and so
 * the sort and search tests can compare lists directly.
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InventoryItem)) {
            return false;
        }
        InventoryItem that = (InventoryItem) other;
        return id == that.id
                && quantity == that.quantity
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(id);
        result = 31 * result + name.hashCode();
        result = 31 * result + quantity;
        return result;
    }

    @Override
    public String toString() {
        return "InventoryItem{id=" + id + ", name='" + name + "', quantity=" + quantity + "}";
    }
}
