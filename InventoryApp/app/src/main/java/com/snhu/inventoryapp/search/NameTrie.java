package com.snhu.inventoryapp.search;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Prefix index over item names, backed by a trie.
 *
 * Each item name is broken into words. The whole normalized name and every
 * suffix that starts at a word boundary are inserted, so "Widget Box, Large"
 * can be found by typing "wid", "box" or "lar". Matching is case insensitive
 * and ignores leading and trailing whitespace.
 *
 * Why a trie instead of filtering the list on every keystroke:
 * a linear filter costs O(N * L) per query, where N is the number of items
 * and L the average name length, and that cost is paid again for every
 * character typed. A prefix walk here costs O(P) to reach the node for a P
 * character query plus O(K) to collect the K nodes below it, and it does not
 * grow with the size of the inventory. The trade off is memory: the index
 * holds one node per distinct character path, which is bounded by the total
 * number of characters across all indexed keys.
 *
 * This class has no Android dependencies so it runs under plain JUnit.
 */
public final class NameTrie {

    /** One node per character. Ids are stored on the node where a key ends. */
    private static final class Node {
        final Map<Character, Node> children = new HashMap<>();
        final Set<Long> idsEndingHere = new HashSet<>();

        boolean isEmpty() {
            return children.isEmpty() && idsEndingHere.isEmpty();
        }
    }

    private final Node root = new Node();
    private final Set<Long> indexedIds = new HashSet<>();
    private int nodeCount = 1;

    /** Number of distinct item ids in the index. */
    public int size() {
        return indexedIds.size();
    }

    /** Number of trie nodes including the root. Useful for reasoning about memory. */
    public int nodeCount() {
        return nodeCount;
    }

    public boolean isEmpty() {
        return indexedIds.isEmpty();
    }

    /** Removes every key and id. */
    public void clear() {
        root.children.clear();
        root.idsEndingHere.clear();
        indexedIds.clear();
        nodeCount = 1;
    }

    /**
     * Indexes an item under its name. Inserting the same id under the same
     * name twice is a no op. Inserting the same id under a different name
     * without removing the old one first leaves both keys pointing at the id,
     * so callers that rename should call {@link #remove} first.
     */
    public void insert(String name, long id) {
        String normalized = normalize(name);
        if (normalized.isEmpty()) {
            return;
        }
        for (int start : wordStarts(normalized)) {
            insertKey(normalized, start, id);
        }
        indexedIds.add(id);
    }

    /**
     * Removes the id from every key derived from the name. Nodes that no
     * longer lead to any id are pruned so the index does not grow forever
     * as items come and go. Returns true when the id was present.
     */
    public boolean remove(String name, long id) {
        String normalized = normalize(name);
        if (normalized.isEmpty() || !indexedIds.contains(id)) {
            return false;
        }
        boolean removedAny = false;
        for (int start : wordStarts(normalized)) {
            removedAny |= removeKey(root, normalized, start, id);
        }
        if (removedAny) {
            indexedIds.remove(id);
        }
        return removedAny;
    }

    /**
     * Returns the ids of every item whose name, or any word in its name,
     * starts with the given prefix. An empty or blank prefix matches every
     * indexed id. The returned set is a fresh copy the caller may keep.
     */
    public Set<Long> findIdsWithPrefix(String prefix) {
        String normalized = normalize(prefix);
        Node node = root;
        for (int i = 0; i < normalized.length(); i++) {
            node = node.children.get(normalized.charAt(i));
            if (node == null) {
                return new HashSet<>();
            }
        }
        return collectIds(node);
    }

    /** True when at least one indexed name starts with the prefix. */
    public boolean hasPrefix(String prefix) {
        String normalized = normalize(prefix);
        Node node = root;
        for (int i = 0; i < normalized.length(); i++) {
            node = node.children.get(normalized.charAt(i));
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    // -------------------- internals --------------------

    private void insertKey(String key, int start, long id) {
        Node node = root;
        for (int i = start; i < key.length(); i++) {
            char c = key.charAt(i);
            Node next = node.children.get(c);
            if (next == null) {
                next = new Node();
                node.children.put(c, next);
                nodeCount++;
            }
            node = next;
        }
        node.idsEndingHere.add(id);
    }

    /**
     * Recursive removal. Returns true if the id was removed under this key.
     * Prunes child nodes on the way back up when they have become empty.
     */
    private boolean removeKey(Node node, String key, int index, long id) {
        if (index == key.length()) {
            return node.idsEndingHere.remove(id);
        }
        char c = key.charAt(index);
        Node child = node.children.get(c);
        if (child == null) {
            return false;
        }
        boolean removed = removeKey(child, key, index + 1, id);
        if (removed && child.isEmpty()) {
            node.children.remove(c);
            nodeCount--;
        }
        return removed;
    }

    /** Iterative depth first walk from a node gathering every id below it. */
    private static Set<Long> collectIds(Node start) {
        Set<Long> ids = new HashSet<>();
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            ids.addAll(node.idsEndingHere);
            for (Node child : node.children.values()) {
                stack.push(child);
            }
        }
        return ids;
    }

    /** Lower case, trimmed, with runs of whitespace collapsed to one space. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Start offsets of every word in an already normalized key. */
    private static Collection<Integer> wordStarts(String key) {
        Deque<Integer> starts = new ArrayDeque<>();
        starts.add(0);
        for (int i = 1; i < key.length(); i++) {
            if (key.charAt(i - 1) == ' ' && key.charAt(i) != ' ') {
                starts.add(i);
            }
        }
        return starts;
    }
}
