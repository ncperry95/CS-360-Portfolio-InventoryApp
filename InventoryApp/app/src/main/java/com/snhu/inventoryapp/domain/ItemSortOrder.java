package com.snhu.inventoryapp.domain;

import com.snhu.inventoryapp.InventoryItem;

import java.util.Comparator;
import java.util.Locale;

/**
 * The sort orders the inventory grid offers, each backed by a Comparator.
 *
 * Every comparator ends with a tie breaker on id so the order is total and
 * deterministic: two items with the same name or the same quantity always
 * come out in the same relative position, no matter what order the database
 * handed them over in. The comparators are pure Java so they can be unit
 * tested without an emulator, and callers use the standard library sort,
 * which is a stable O(n log n) merge sort.
 *
 * "Newest first" uses the row id. The table has no created_at column, and
 * ids are assigned by AUTOINCREMENT, so a higher id always means a later
 * insert. Adding a real timestamp is a schema change and belongs with the
 * database enhancement.
 */
public enum ItemSortOrder {

    /** Case insensitive A to Z. */
    NAME_ASC(Comparators.BY_NAME),

    /** Lowest quantity first, so what needs restocking floats to the top. */
    QUANTITY_ASC(Comparators.BY_QUANTITY),

    /** Most recently added first. */
    NEWEST_FIRST(Comparators.BY_NEWEST);

    /**
     * Enum constants cannot read static fields of their own enum during
     * construction, so the comparators live in a nested holder.
     */
    private static final class Comparators {

        static final Comparator<InventoryItem> NAME_KEY =
                Comparator.comparing((InventoryItem item) -> item.getName().toLowerCase(Locale.ROOT));

        static final Comparator<InventoryItem> BY_NAME = NAME_KEY
                .thenComparing(InventoryItem::getName)
                .thenComparingLong(InventoryItem::getId);

        static final Comparator<InventoryItem> BY_QUANTITY =
                Comparator.comparingInt(InventoryItem::getQuantity)
                        .thenComparing(BY_NAME);

        static final Comparator<InventoryItem> BY_NEWEST =
                Comparator.comparingLong(InventoryItem::getId).reversed();

        private Comparators() {
        }
    }

    private final Comparator<InventoryItem> comparator;

    ItemSortOrder(Comparator<InventoryItem> comparator) {
        this.comparator = comparator;
    }

    public Comparator<InventoryItem> comparator() {
        return comparator;
    }
}
