package com.snhu.inventoryapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class InventoryItemTest {

    @Test
    public void equalWhenAllFieldsMatch() {
        InventoryItem a = new InventoryItem(1, "Bolt", 5);
        InventoryItem b = new InventoryItem(1, "Bolt", 5);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void quantityChangeIsAContentChange() {
        // Same row, different quantity: DiffUtil must rebind this cell.
        assertNotEquals(new InventoryItem(1, "Bolt", 5), new InventoryItem(1, "Bolt", 4));
    }

    @Test
    public void nameChangeIsAContentChange() {
        assertNotEquals(new InventoryItem(1, "Bolt", 5), new InventoryItem(1, "Bolts", 5));
    }

    @Test
    public void differentIdIsADifferentItem() {
        assertNotEquals(new InventoryItem(1, "Bolt", 5), new InventoryItem(2, "Bolt", 5));
    }

    @Test
    public void notEqualToNullOrOtherTypes() {
        InventoryItem item = new InventoryItem(1, "Bolt", 5);
        assertNotEquals(item, null);
        assertNotEquals(item, "Bolt");
    }
}
