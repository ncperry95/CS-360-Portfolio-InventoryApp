package com.snhu.inventoryapp.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.snhu.inventoryapp.InventoryItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LowStockQueueTest {

    private static final InventoryItem EMPTY_BIN = new InventoryItem(1, "Empty bin", 0);
    private static final InventoryItem WASHERS = new InventoryItem(2, "Washers", 2);
    private static final InventoryItem NUTS = new InventoryItem(3, "Nuts", 4);
    private static final InventoryItem AT_THRESHOLD = new InventoryItem(4, "Screws",
            LowStockRule.THRESHOLD);
    private static final InventoryItem PLENTY = new InventoryItem(5, "Tape", 90);

    private static LowStockQueue queueOf(InventoryItem... items) {
        List<InventoryItem> list = new ArrayList<>(Arrays.asList(items));
        Collections.shuffle(list);
        LowStockQueue queue = new LowStockQueue();
        queue.rebuild(list);
        return queue;
    }

    @Test
    public void lowestIsAtTheTopRegardlessOfInsertOrder() {
        LowStockQueue queue = queueOf(PLENTY, NUTS, EMPTY_BIN, WASHERS, AT_THRESHOLD);
        assertEquals(EMPTY_BIN, queue.peekLowest());
        assertEquals(5, queue.size());
    }

    @Test
    public void lowStockItemsAreExactlyThoseBelowThresholdInAscendingOrder() {
        LowStockQueue queue = queueOf(PLENTY, NUTS, EMPTY_BIN, WASHERS, AT_THRESHOLD);
        assertEquals(Arrays.asList(EMPTY_BIN, WASHERS, NUTS), queue.lowStockItems());
        // At the threshold is not low; matches LowStockRule.
        assertFalse(queue.lowStockItems().contains(AT_THRESHOLD));
        assertEquals(3, queue.lowStockCount());
    }

    @Test
    public void readingLowStockDoesNotDrainTheHeap() {
        LowStockQueue queue = queueOf(PLENTY, NUTS, EMPTY_BIN, WASHERS);
        queue.lowStockItems();
        queue.lowestLowStock(2);
        assertEquals(4, queue.size());
        assertEquals(EMPTY_BIN, queue.peekLowest());
    }

    @Test
    public void lowestLowStockHonorsTheLimit() {
        LowStockQueue queue = queueOf(PLENTY, NUTS, EMPTY_BIN, WASHERS);
        assertEquals(Arrays.asList(EMPTY_BIN, WASHERS), queue.lowestLowStock(2));
        assertEquals(Arrays.asList(EMPTY_BIN, WASHERS, NUTS), queue.lowestLowStock(10));
        assertTrue(queue.lowestLowStock(0).isEmpty());
    }

    @Test
    public void hasLowStockIsFalseWhenEverythingIsStocked() {
        LowStockQueue queue = queueOf(PLENTY, AT_THRESHOLD);
        assertFalse(queue.hasLowStock());
        assertTrue(queue.lowStockItems().isEmpty());
        assertEquals(0, queue.lowStockCount());
        assertEquals(AT_THRESHOLD, queue.peekLowest());
    }

    @Test
    public void emptyQueueIsSafe() {
        LowStockQueue queue = new LowStockQueue();
        assertTrue(queue.isEmpty());
        assertNull(queue.peekLowest());
        assertFalse(queue.hasLowStock());
        assertTrue(queue.lowStockItems().isEmpty());
        assertEquals(0, queue.lowStockCount());
        queue.rebuild(Collections.<InventoryItem>emptyList());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void offerAndRemoveKeepTheOrder() {
        LowStockQueue queue = queueOf(PLENTY, AT_THRESHOLD);
        assertFalse(queue.hasLowStock());
        queue.offer(WASHERS);
        assertTrue(queue.hasLowStock());
        assertEquals(WASHERS, queue.peekLowest());
        queue.offer(EMPTY_BIN);
        assertEquals(EMPTY_BIN, queue.peekLowest());
        assertTrue(queue.remove(EMPTY_BIN));
        assertEquals(WASHERS, queue.peekLowest());
        assertFalse(queue.remove(EMPTY_BIN));
    }

    @Test
    public void rebuildReplacesEverything() {
        LowStockQueue queue = queueOf(EMPTY_BIN, WASHERS);
        queue.rebuild(Collections.singletonList(PLENTY));
        assertEquals(1, queue.size());
        assertFalse(queue.hasLowStock());
    }

    @Test
    public void tiesOnQuantityAreBrokenByName() {
        InventoryItem zebra = new InventoryItem(20, "Zebra strips", 1);
        InventoryItem anchor = new InventoryItem(21, "Anchor bolts", 1);
        LowStockQueue queue = queueOf(zebra, anchor);
        assertEquals(Arrays.asList(anchor, zebra), queue.lowStockItems());
    }
}
