package com.snhu.inventoryapp.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.search.NameTrie;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InventoryQueryTest {

    private static final InventoryItem WIDGET_L = new InventoryItem(1, "Widget Box, Large", 3);
    private static final InventoryItem WIDGET_S = new InventoryItem(2, "Widget Box, Small", 30);
    private static final InventoryItem WINGNUT = new InventoryItem(3, "Wingnut", 8);
    private static final InventoryItem BOLT = new InventoryItem(4, "Bolt", 1);

    private List<InventoryItem> all;
    private NameTrie index;

    @Before
    public void setUp() {
        // Database order is by id; hand it over scrambled to prove sorting.
        all = Arrays.asList(WINGNUT, BOLT, WIDGET_S, WIDGET_L);
        index = new NameTrie();
        for (InventoryItem item : all) {
            index.insert(item.getName(), item.getId());
        }
    }

    @Test
    public void blankQueryReturnsEverythingSorted() {
        assertEquals(Arrays.asList(BOLT, WIDGET_L, WIDGET_S, WINGNUT),
                InventoryQuery.apply(all, index, "", ItemSortOrder.NAME_ASC));
        assertEquals(Arrays.asList(BOLT, WIDGET_L, WIDGET_S, WINGNUT),
                InventoryQuery.apply(all, index, null, ItemSortOrder.NAME_ASC));
        assertEquals(Arrays.asList(BOLT, WIDGET_L, WINGNUT, WIDGET_S),
                InventoryQuery.apply(all, index, "  ", ItemSortOrder.QUANTITY_ASC));
    }

    @Test
    public void queryFiltersThroughTheIndexThenSorts() {
        assertEquals(Arrays.asList(WIDGET_L, WIDGET_S),
                InventoryQuery.apply(all, index, "wid", ItemSortOrder.NAME_ASC));
        assertEquals(Arrays.asList(WIDGET_S, WIDGET_L),
                InventoryQuery.apply(all, index, "box", ItemSortOrder.NEWEST_FIRST));
        assertEquals(Arrays.asList(WIDGET_L, WINGNUT, WIDGET_S),
                InventoryQuery.apply(all, index, "wi", ItemSortOrder.QUANTITY_ASC));
    }

    @Test
    public void noMatchGivesAnEmptyList() {
        assertTrue(InventoryQuery.apply(all, index, "xyz", ItemSortOrder.NAME_ASC).isEmpty());
    }

    @Test
    public void queryIsAppliedToWhateverBaseListIsGiven() {
        // The ViewModel passes only the low stock items when that filter is on.
        List<InventoryItem> lowOnly = Arrays.asList(BOLT, WIDGET_L);
        assertEquals(Collections.singletonList(WIDGET_L),
                InventoryQuery.apply(lowOnly, index, "w", ItemSortOrder.NAME_ASC));
    }

    @Test
    public void inputListIsNeverMutatedOrReturned() {
        List<InventoryItem> copy = Arrays.asList(WINGNUT, BOLT, WIDGET_S, WIDGET_L);
        List<InventoryItem> result = InventoryQuery.apply(all, index, "", ItemSortOrder.NAME_ASC);
        assertNotSame(all, result);
        assertEquals(copy, all);
    }
}
