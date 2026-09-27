package com.snhu.inventoryapp.search;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.domain.ItemSortOrder;
import com.snhu.inventoryapp.domain.LowStockQueue;
import com.snhu.inventoryapp.domain.LowStockRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Not a unit test. A plain main method that times the two approaches to
 * search and low stock on generated inventories so the enhancement
 * narrative can quote measured numbers instead of only big O.
 *
 * Run from the test source set with:
 *   java -cp <test classes>:<main classes> com.snhu.inventoryapp.search.SearchBenchmark
 *
 * Search compares filtering the list on every keystroke (what the app would
 * have to do without an index) against walking the trie. Both match a prefix
 * of the whole name or of any word, so they return the same ids.
 *
 * Low stock compares sorting the whole list by quantity and taking the head
 * against a min heap peek plus pulling the k items that are below threshold.
 */
public final class SearchBenchmark {

    private static final String[] WORDS = {
            "Widget", "Bolt", "Nut", "Washer", "Bracket", "Hinge", "Cable", "Fuse",
            "Relay", "Sensor", "Gasket", "Spring", "Bearing", "Valve", "Filter",
            "Clamp", "Screw", "Anchor", "Rivet", "Coupler", "Adapter", "Panel",
            "Large", "Small", "Steel", "Brass", "Nylon", "Black", "Red", "Blue"
    };

    /** Grouped by length: short prefixes match a lot, long ones match little. */
    private static final String[][] QUERY_GROUPS = {
            {"w", "b", "s", "c"},
            {"wi", "br", "st", "cl"},
            {"widg", "brac", "stee", "clam"}
    };
    private static final String[] QUERIES = {
            "w", "b", "s", "c", "wi", "br", "st", "cl", "widg", "brac", "stee", "clam"
    };

    private SearchBenchmark() {
    }

    public static void main(String[] args) {
        int[] sizes = {100, 1_000, 10_000, 100_000};
        System.out.println("items | prefix length | linear filter per keystroke | trie per keystroke | matches | trie build | trie nodes");
        for (int n : sizes) {
            List<InventoryItem> items = generate(n, 42L);
            runSearch(n, items);
        }
        System.out.println();
        System.out.println("items | sort whole list, take low | heap rebuild | heap peek | heap pull low (k)");
        for (int n : sizes) {
            List<InventoryItem> items = generate(n, 42L);
            runLowStock(n, items);
        }
    }

    // -------------------- search --------------------

    private static void runSearch(int n, List<InventoryItem> items) {
        // Warm up both paths so the JIT has settled before timing.
        NameTrie warm = build(items);
        for (int i = 0; i < 200; i++) {
            for (String q : QUERIES) {
                linearFilter(items, q);
                warm.findIdsWithPrefix(q);
            }
        }

        long buildStart = System.nanoTime();
        NameTrie trie = build(items);
        long buildNanos = System.nanoTime() - buildStart;

        // Sanity: both approaches must agree on every query.
        for (String q : QUERIES) {
            if (!linearFilter(items, q).equals(trie.findIdsWithPrefix(q))) {
                throw new AssertionError("mismatch on query " + q);
            }
        }

        int rounds = 100;
        for (String[] group : QUERY_GROUPS) {
            long linearStart = System.nanoTime();
            for (int i = 0; i < rounds; i++) {
                for (String q : group) {
                    linearFilter(items, q);
                }
            }
            long linearNanos = (System.nanoTime() - linearStart) / (rounds * group.length);

            long trieStart = System.nanoTime();
            int matches = 0;
            for (int i = 0; i < rounds; i++) {
                for (String q : group) {
                    matches += trie.findIdsWithPrefix(q).size();
                }
            }
            long trieNanos = (System.nanoTime() - trieStart) / (rounds * group.length);
            matches /= rounds * group.length;

            System.out.printf(Locale.ROOT, "%,7d | %13d | %10s | %10s | %,7d | %10s | %,d%n",
                    n, group[0].length(), micros(linearNanos), micros(trieNanos), matches,
                    micros(buildNanos), trie.nodeCount());
        }
    }

    private static NameTrie build(List<InventoryItem> items) {
        NameTrie trie = new NameTrie();
        for (InventoryItem item : items) {
            trie.insert(item.getName(), item.getId());
        }
        return trie;
    }

    /** What the app would do without an index: scan every name on every keystroke. */
    private static Set<Long> linearFilter(List<InventoryItem> items, String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        Set<Long> matches = new HashSet<>();
        for (InventoryItem item : items) {
            String name = item.getName().toLowerCase(Locale.ROOT);
            if (name.startsWith(q)) {
                matches.add(item.getId());
                continue;
            }
            int space = name.indexOf(' ');
            while (space >= 0) {
                if (name.startsWith(q, space + 1)) {
                    matches.add(item.getId());
                    break;
                }
                space = name.indexOf(' ', space + 1);
            }
        }
        return matches;
    }

    // -------------------- low stock --------------------

    private static void runLowStock(int n, List<InventoryItem> items) {
        LowStockQueue warm = new LowStockQueue();
        for (int i = 0; i < 50; i++) {
            sortAndTakeLow(items);
            warm.rebuild(items);
            warm.lowStockItems();
        }

        int rounds = 50;
        long sortStart = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            sortAndTakeLow(items);
        }
        long sortNanos = (System.nanoTime() - sortStart) / rounds;

        long rebuildStart = System.nanoTime();
        LowStockQueue queue = null;
        for (int i = 0; i < rounds; i++) {
            queue = new LowStockQueue();
            queue.rebuild(items);
        }
        long rebuildNanos = (System.nanoTime() - rebuildStart) / rounds;

        long peekStart = System.nanoTime();
        for (int i = 0; i < rounds * 1000; i++) {
            queue.peekLowest();
        }
        long peekNanos = (System.nanoTime() - peekStart) / (rounds * 1000);

        long pullStart = System.nanoTime();
        int k = 0;
        for (int i = 0; i < rounds; i++) {
            k = queue.lowStockItems().size();
        }
        long pullNanos = (System.nanoTime() - pullStart) / rounds;

        System.out.printf(Locale.ROOT, "%,7d | %10s | %10s | %10s | %10s (%,d)%n",
                n, micros(sortNanos), micros(rebuildNanos), micros(peekNanos), micros(pullNanos), k);
    }

    private static List<InventoryItem> sortAndTakeLow(List<InventoryItem> items) {
        List<InventoryItem> copy = new ArrayList<>(items);
        Collections.sort(copy, ItemSortOrder.QUANTITY_ASC.comparator());
        List<InventoryItem> low = new ArrayList<>();
        for (InventoryItem item : copy) {
            if (!LowStockRule.isLowStock(item.getQuantity())) {
                break;
            }
            low.add(item);
        }
        return low;
    }

    // -------------------- data --------------------

    private static List<InventoryItem> generate(int n, long seed) {
        Random random = new Random(seed);
        List<InventoryItem> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int words = 1 + random.nextInt(3);
            StringBuilder name = new StringBuilder();
            for (int w = 0; w < words; w++) {
                if (w > 0) {
                    name.append(' ');
                }
                name.append(WORDS[random.nextInt(WORDS.length)]);
            }
            name.append(' ').append(i);
            // Roughly one in twenty items is low on stock.
            int quantity = random.nextInt(20) == 0
                    ? random.nextInt(LowStockRule.THRESHOLD)
                    : LowStockRule.THRESHOLD + random.nextInt(200);
            items.add(new InventoryItem(i + 1, name.toString(), quantity));
        }
        return items;
    }

    private static String micros(long nanos) {
        if (nanos < 1_000) {
            return nanos + " ns";
        }
        if (nanos < 1_000_000) {
            return String.format(Locale.ROOT, "%.1f us", nanos / 1_000.0);
        }
        return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0);
    }
}
