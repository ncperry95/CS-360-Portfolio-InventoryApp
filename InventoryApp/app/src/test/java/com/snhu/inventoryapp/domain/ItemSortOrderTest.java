package com.snhu.inventoryapp.domain;

import static org.junit.Assert.assertEquals;

import com.snhu.inventoryapp.InventoryItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemSortOrderTest {

    private static final InventoryItem BOLT = new InventoryItem(3, "Bolt", 12);
    private static final InventoryItem APPLE = new InventoryItem(7, "apple", 2);
    private static final InventoryItem ZIP_TIE = new InventoryItem(1, "Zip Tie", 2);
    private static final InventoryItem BANANA = new InventoryItem(9, "Banana", 40);
    private static final InventoryItem BOLT_TWO = new InventoryItem(11, "Bolt", 12);

    private static List<InventoryItem> shuffled() {
        List<InventoryItem> items = new ArrayList<>(
                Arrays.asList(BOLT, APPLE, ZIP_TIE, BANANA, BOLT_TWO));
        Collections.shuffle(items);
        return items;
    }

    private static List<InventoryItem> sortedBy(ItemSortOrder order) {
        List<InventoryItem> items = shuffled();
        Collections.sort(items, order.comparator());
        return items;
    }

    @Test
    public void nameAscIsCaseInsensitive() {
        // Lower case "apple" sorts before "Banana", not after every capital.
        assertEquals(Arrays.asList(APPLE, BANANA, BOLT, BOLT_TWO, ZIP_TIE),
                sortedBy(ItemSortOrder.NAME_ASC));
    }

    @Test
    public void nameAscBreaksTiesById() {
        // Same name: lower row id first, every time.
        List<InventoryItem> result = sortedBy(ItemSortOrder.NAME_ASC);
        assertEquals(BOLT, result.get(2));
        assertEquals(BOLT_TWO, result.get(3));
    }

    @Test
    public void quantityAscThenNameThenId() {
        // apple and Zip Tie tie at 2: name decides. Bolts tie at 12: id decides.
        assertEquals(Arrays.asList(APPLE, ZIP_TIE, BOLT, BOLT_TWO, BANANA),
                sortedBy(ItemSortOrder.QUANTITY_ASC));
    }

    @Test
    public void newestFirstIsDescendingId() {
        assertEquals(Arrays.asList(BOLT_TWO, BANANA, APPLE, BOLT, ZIP_TIE),
                sortedBy(ItemSortOrder.NEWEST_FIRST));
    }

    @Test
    public void everyOrderIsDeterministicAcrossRuns() {
        for (ItemSortOrder order : ItemSortOrder.values()) {
            List<InventoryItem> first = sortedBy(order);
            for (int i = 0; i < 20; i++) {
                assertEquals("order " + order, first, sortedBy(order));
            }
        }
    }

    @Test
    public void comparatorsAreConsistentWithEquals() {
        InventoryItem copy = new InventoryItem(3, "Bolt", 12);
        for (ItemSortOrder order : ItemSortOrder.values()) {
            assertEquals(0, order.comparator().compare(BOLT, copy));
        }
    }
}
