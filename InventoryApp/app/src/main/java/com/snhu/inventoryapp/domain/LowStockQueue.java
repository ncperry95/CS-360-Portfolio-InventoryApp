package com.snhu.inventoryapp.domain;

import com.snhu.inventoryapp.InventoryItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A min heap of inventory items keyed on quantity, so the items closest to
 * running out are always at the top.
 *
 * The grid only ever needs the few lowest items: a banner with the two or
 * three most urgent names, and a "low stock" view listing everything under
 * the threshold. A heap fits that access pattern better than sorting the
 * whole list. Looking at the single lowest item is O(1), and pulling out the
 * k lowest is O(k log n) on top of an O(n) copy, so the cost tracks how many
 * items are actually low rather than how many items exist. A full sort would
 * be O(n log n) every time and would order items nobody is going to look at.
 *
 * Ties on quantity are broken by name and then id (see
 * {@link ItemSortOrder#QUANTITY_ASC}) so the heap order is deterministic.
 *
 * Not thread safe. The ViewModel guards it with its own lock.
 */
public final class LowStockQueue {

    private static final Comparator<InventoryItem> ORDER = ItemSortOrder.QUANTITY_ASC.comparator();

    private PriorityQueue<InventoryItem> heap = new PriorityQueue<>(ORDER);

    /**
     * Replaces the contents with the given items. O(n log n): the
     * PriorityQueue collection constructor only heapifies in linear time
     * when it is not given a comparator, so this adds one at a time. That
     * happens once per database load, not once per keystroke or tap.
     */
    public void rebuild(Collection<InventoryItem> items) {
        PriorityQueue<InventoryItem> fresh = new PriorityQueue<>(Math.max(11, items.size()), ORDER);
        fresh.addAll(items);
        heap = fresh;
    }

    /** O(log n). */
    public void offer(InventoryItem item) {
        heap.offer(item);
    }

    /** O(n): the heap has no index, so removal is a scan. */
    public boolean remove(InventoryItem item) {
        return heap.remove(item);
    }

    /** The item with the lowest quantity, or null when empty. O(1). */
    public InventoryItem peekLowest() {
        return heap.peek();
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /** True when the lowest item is below the threshold. O(1). */
    public boolean hasLowStock() {
        InventoryItem lowest = heap.peek();
        return lowest != null && LowStockRule.isLowStock(lowest.getQuantity());
    }

    /**
     * Every item below the threshold, lowest quantity first. The heap is
     * left intact. O(n) to copy the heap plus O(k log n) to pull k items.
     */
    public List<InventoryItem> lowStockItems() {
        return lowestWhile(Integer.MAX_VALUE);
    }

    /**
     * Up to {@code limit} of the lowest items that are below the threshold,
     * lowest first. Used for the banner. O(n + limit log n).
     */
    public List<InventoryItem> lowestLowStock(int limit) {
        return lowestWhile(limit);
    }

    /** Number of items below the threshold. O(n) scan, no ordering needed. */
    public int lowStockCount() {
        int count = 0;
        for (InventoryItem item : heap) {
            if (LowStockRule.isLowStock(item.getQuantity())) {
                count++;
            }
        }
        return count;
    }

    private List<InventoryItem> lowestWhile(int limit) {
        List<InventoryItem> result = new ArrayList<>();
        if (limit <= 0 || heap.isEmpty()) {
            return result;
        }
        PriorityQueue<InventoryItem> copy = new PriorityQueue<>(heap);
        while (!copy.isEmpty() && result.size() < limit) {
            InventoryItem next = copy.poll();
            if (!LowStockRule.isLowStock(next.getQuantity())) {
                break;
            }
            result.add(next);
        }
        return result;
    }
}
