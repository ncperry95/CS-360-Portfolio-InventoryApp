package com.snhu.inventoryapp.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LowStockRuleTest {

    @Test
    public void thresholdIsExclusive() {
        assertFalse(LowStockRule.isLowStock(LowStockRule.THRESHOLD));
        assertTrue(LowStockRule.isLowStock(LowStockRule.THRESHOLD - 1));
        assertTrue(LowStockRule.isLowStock(0));
        assertFalse(LowStockRule.isLowStock(100));
    }

    @Test
    public void crossingFiresOnlyOnTheDownwardTransition() {
        // 5 -> 4 crosses
        assertTrue(LowStockRule.crossedBelowThreshold(5, 4));
        // 4 -> 3 is already low, no second alert
        assertFalse(LowStockRule.crossedBelowThreshold(4, 3));
        // 4 -> 5 going back up never fires
        assertFalse(LowStockRule.crossedBelowThreshold(4, 5));
        // 6 -> 5 stays at the threshold, not below it
        assertFalse(LowStockRule.crossedBelowThreshold(6, 5));
        // A big jump from high to low still fires once
        assertTrue(LowStockRule.crossedBelowThreshold(50, 0));
    }
}
