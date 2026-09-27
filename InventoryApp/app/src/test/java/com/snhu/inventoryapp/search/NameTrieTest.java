package com.snhu.inventoryapp.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NameTrieTest {

    private NameTrie trie;

    @Before
    public void setUp() {
        trie = new NameTrie();
        trie.insert("Widget Box, Large", 1L);
        trie.insert("Widget Box, Small", 2L);
        trie.insert("Wingnut", 3L);
        trie.insert("Bolt", 4L);
        trie.insert("bolt cutter", 5L);
    }

    private static Set<Long> ids(Long... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @Test
    public void prefixOfTheWholeNameMatches() {
        assertEquals(ids(1L, 2L), trie.findIdsWithPrefix("widget"));
        assertEquals(ids(1L, 2L, 3L), trie.findIdsWithPrefix("wi"));
        assertEquals(ids(1L), trie.findIdsWithPrefix("widget box, l"));
    }

    @Test
    public void prefixOfAnyWordMatches() {
        // "box" is the second word of both widget names.
        assertEquals(ids(1L, 2L), trie.findIdsWithPrefix("box"));
        assertEquals(ids(1L), trie.findIdsWithPrefix("lar"));
        // "cutter" is the second word of id 5 only.
        assertEquals(ids(5L), trie.findIdsWithPrefix("cut"));
    }

    @Test
    public void matchingIsCaseInsensitiveAndTrimmed() {
        assertEquals(ids(4L, 5L), trie.findIdsWithPrefix("BOLT"));
        // "bo" also reaches "box" inside the two widget names.
        assertEquals(ids(1L, 2L, 4L, 5L), trie.findIdsWithPrefix("  bo  "));
        assertEquals(ids(4L, 5L), trie.findIdsWithPrefix("  bol  "));
        assertEquals(ids(1L, 2L, 3L), trie.findIdsWithPrefix("W"));
    }

    @Test
    public void noMatchReturnsEmptyNotNull() {
        Set<Long> result = trie.findIdsWithPrefix("zzz");
        assertTrue(result.isEmpty());
        assertFalse(trie.hasPrefix("zzz"));
        // Longer than any key.
        assertTrue(trie.findIdsWithPrefix("wingnuts").isEmpty());
    }

    @Test
    public void blankPrefixMatchesEverything() {
        assertEquals(ids(1L, 2L, 3L, 4L, 5L), trie.findIdsWithPrefix(""));
        assertEquals(ids(1L, 2L, 3L, 4L, 5L), trie.findIdsWithPrefix("   "));
        assertEquals(ids(1L, 2L, 3L, 4L, 5L), trie.findIdsWithPrefix(null));
    }

    @Test
    public void sameNameDifferentIdsBothFound() {
        trie.insert("Bolt", 6L);
        assertEquals(ids(4L, 5L, 6L), trie.findIdsWithPrefix("bolt"));
        assertEquals(6, trie.size());
    }

    @Test
    public void duplicateInsertIsANoOp() {
        int nodesBefore = trie.nodeCount();
        trie.insert("Bolt", 4L);
        assertEquals(nodesBefore, trie.nodeCount());
        assertEquals(5, trie.size());
        assertEquals(ids(4L, 5L), trie.findIdsWithPrefix("bolt"));
    }

    @Test
    public void removeDropsTheIdFromEveryWordKey() {
        assertTrue(trie.remove("Widget Box, Large", 1L));
        assertEquals(ids(2L), trie.findIdsWithPrefix("widget"));
        assertEquals(ids(2L), trie.findIdsWithPrefix("box"));
        assertTrue(trie.findIdsWithPrefix("large").isEmpty());
        assertEquals(4, trie.size());
    }

    @Test
    public void removePrunesNodesNoOneElseUses() {
        int nodesBefore = trie.nodeCount();
        // "Wingnut" shares "wi" with the widget names but "ngnut" is its own.
        assertTrue(trie.remove("Wingnut", 3L));
        assertTrue(trie.nodeCount() < nodesBefore);
        // The shared "wi" path must survive.
        assertEquals(ids(1L, 2L), trie.findIdsWithPrefix("wi"));
        assertFalse(trie.hasPrefix("win"));
    }

    @Test
    public void removeOfUnknownIdOrNameReturnsFalse() {
        assertFalse(trie.remove("Bolt", 99L));
        // Right id, wrong name: nothing is stored under this key.
        assertFalse(trie.remove("Bolts", 4L));
        assertEquals(5, trie.size());
    }

    @Test
    public void insertThenRemoveEverythingReturnsToEmpty() {
        NameTrie fresh = new NameTrie();
        assertTrue(fresh.isEmpty());
        assertEquals(1, fresh.nodeCount());
        fresh.insert("Alpha Beta", 10L);
        assertFalse(fresh.isEmpty());
        assertTrue(fresh.remove("Alpha Beta", 10L));
        assertTrue(fresh.isEmpty());
        assertEquals(1, fresh.nodeCount());
    }

    @Test
    public void clearEmptiesTheIndex() {
        trie.clear();
        assertTrue(trie.isEmpty());
        assertEquals(1, trie.nodeCount());
        assertTrue(trie.findIdsWithPrefix("w").isEmpty());
    }

    @Test
    public void resultIsACopyTheCallerMayMutate() {
        Set<Long> result = trie.findIdsWithPrefix("bolt");
        result.clear();
        assertEquals(ids(4L, 5L), trie.findIdsWithPrefix("bolt"));
    }

    @Test
    public void extraWhitespaceInsideNamesIsCollapsed() {
        trie.insert("  Hex   Nut ", 7L);
        assertEquals(ids(7L), trie.findIdsWithPrefix("hex nut"));
        assertEquals(ids(7L), trie.findIdsWithPrefix("nut"));
        assertTrue(trie.remove("Hex Nut", 7L));
    }
}
