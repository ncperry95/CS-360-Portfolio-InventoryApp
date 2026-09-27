package com.snhu.inventoryapp.domain;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.search.NameTrie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Turns the full item list into what the grid should show: filtered by the
 * search text through the prefix index, then sorted by the chosen order.
 *
 * Pure function of its inputs, so it is unit tested directly and the
 * ViewModel only has to hold state and call it. The search text is never
 * sent to the database; it is matched in memory against the trie, so there
 * is no path from the search box to a SQL statement.
 */
public final class InventoryQuery {

    private InventoryQuery() {
    }

    /**
     * @param items the candidate items, in any order
     * @param index a prefix index over the same items
     * @param query search text; blank means no filter
     * @param order how to sort the result
     * @return a new list, never the input
     */
    public static List<InventoryItem> apply(List<InventoryItem> items,
                                            NameTrie index,
                                            String query,
                                            ItemSortOrder order) {
        List<InventoryItem> result;
        if (isBlank(query)) {
            result = new ArrayList<>(items);
        } else {
            // O(P + K) to get the matching ids, then one O(n) pass to keep
            // only those items. The pass is over ids, not name characters,
            // so it stays cheap as names get longer.
            Set<Long> matches = index.findIdsWithPrefix(query);
            result = new ArrayList<>(matches.size());
            for (InventoryItem item : items) {
                if (matches.contains(item.getId())) {
                    result.add(item);
                }
            }
        }
        // Stable O(n log n) merge sort from the standard library.
        Collections.sort(result, order.comparator());
        return result;
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
